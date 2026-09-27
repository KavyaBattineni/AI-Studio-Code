package com.aivideo.platform.video.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Development/demo video provider. Simulates realistic processing latency (proportional to
 * the requested clip duration) and then "completes" with a deterministic, clearly-fake
 * CDN-style URL - no AI_VIDEO_API_KEY required.
 *
 * To trigger the FAILED path on purpose (for testing refund handling end-to-end), include the
 * literal text "FAIL_TEST" anywhere in the prompt. This is documented in the README.
 *
 * Swap this for a real provider adapter (implementing VideoProvider against your chosen AI
 * video API) selected via app.mode=live plus AI_VIDEO_API_KEY. Do not invent a specific
 * vendor's request/response shape here - integrate against the real API's actual documented
 * contract when you have credentials for one.
 */
@Component
@ConditionalOnProperty(name = "app.mode", havingValue = "mock", matchIfMissing = true)
public class MockVideoProvider implements VideoProvider {

    private static final Logger log = LoggerFactory.getLogger(MockVideoProvider.class);
    private final Map<String, VideoGenerationResult> lastKnownStatus = new ConcurrentHashMap<>();

    @Override
    public VideoGenerationResult createVideo(String prompt, int durationSeconds) {
        String providerJobId = "mock-" + UUID.randomUUID();
        log.info("[MOCK VIDEO PROVIDER] Starting generation job={} duration={}s", providerJobId, durationSeconds);

        sleepQuietly(1200); // simulate queueing / cold-start latency

        boolean forcedFailure = prompt != null && prompt.toUpperCase(Locale.ROOT).contains("FAIL_TEST");

        // Longer requested clips take (simulated) longer to generate.
        long generationMillis = 1800L * Math.max(1, durationSeconds / 4);
        sleepQuietly(generationMillis);

        VideoGenerationResult result;
        if (forcedFailure) {
            result = VideoGenerationResult.failed(providerJobId, "Simulated provider failure (prompt contained FAIL_TEST)");
        } else {
            String videoUrl = "https://mock-cdn.aivideostudio.local/videos/" + providerJobId + ".mp4";
            String thumbnailUrl = "https://mock-cdn.aivideostudio.local/thumbnails/" + providerJobId + ".jpg";
            result = VideoGenerationResult.completed(providerJobId, videoUrl, thumbnailUrl);
        }

        lastKnownStatus.put(providerJobId, result);
        log.info("[MOCK VIDEO PROVIDER] Finished job={} status={}", providerJobId, result.status());
        return result;
    }

    @Override
    public VideoGenerationResult getVideoStatus(String providerJobId) {
        VideoGenerationResult known = lastKnownStatus.get(providerJobId);
        if (known != null) {
            return known;
        }
        return new VideoGenerationResult(providerJobId, "PROCESSING", null, null, null);
    }

    @Override
    public void cancelVideo(String providerJobId) {
        log.info("[MOCK VIDEO PROVIDER] Cancel requested for job={}", providerJobId);
        lastKnownStatus.put(providerJobId, new VideoGenerationResult(providerJobId, "FAILED", null, null, "Cancelled by user"));
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
