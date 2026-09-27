package com.aivideo.platform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * A minimal, dependency-free, single-JVM sliding-window rate limiter protecting the
 * endpoints most attractive to abuse (auth, rewards, video generation).
 *
 * ⚠️ PRODUCTION NOTE: this filter keeps counters in local memory, so limits are per
 * instance - running two backend replicas doubles the effective limit, and a restart
 * resets all counters. For a real multi-instance deployment, replace this with a
 * distributed limiter such as Bucket4j backed by Redis, keyed the same way (rule prefix +
 * client identity), and remove this class's registration from SecurityConfig.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private record Rule(String prefix, int limit, Duration window) {
    }

    private final List<Rule> rules = List.of(
            new Rule("/api/auth/login", 10, Duration.ofMinutes(1)),
            new Rule("/api/auth/register", 5, Duration.ofMinutes(1)),
            new Rule("/api/auth/verify-otp", 10, Duration.ofMinutes(1)),
            new Rule("/api/auth/resend-otp", 5, Duration.ofMinutes(1)),
            new Rule("/api/auth/forgot-password", 5, Duration.ofMinutes(1)),
            new Rule("/api/rewards", 20, Duration.ofMinutes(1)),
            new Rule("/api/videos/generate", 10, Duration.ofMinutes(1))
    );

    private final ConcurrentHashMap<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    @Value("${app.rate-limit.enabled:true}")
    private boolean enabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        Optional<Rule> matched = rules.stream().filter(r -> path.startsWith(r.prefix())).findFirst();
        if (matched.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        Rule rule = matched.get();
        String key = rule.prefix() + "|" + clientKey(request);
        long now = System.currentTimeMillis();
        long windowStart = now - rule.window().toMillis();
        Deque<Long> timestamps = hits.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());

        boolean allowed;
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }
            allowed = timestamps.size() < rule.limit();
            if (allowed) {
                timestamps.addLast(now);
            }
        }

        if (!allowed) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Too many requests. Please try again later.\",\"errorCode\":\"RATE_LIMIT_EXCEEDED\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = (forwarded != null && !forwarded.isBlank()) ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
        // Include the authenticated user, if any, so rate limits track a logged-in
        // user across IP changes as well as anonymous callers by IP.
        String auth = request.getHeader("Authorization");
        return ip + "|" + (auth != null ? Integer.toHexString(auth.hashCode()) : "anon");
    }
}
