package com.aivideo.platform.auth;

import com.aivideo.platform.auth.entity.OtpPurpose;
import com.aivideo.platform.auth.entity.OtpVerification;
import com.aivideo.platform.common.HashUtil;
import com.aivideo.platform.common.exception.BadRequestException;
import com.aivideo.platform.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

/**
 * Handles OTP lifecycle: generation, hashed storage, expiry, single-use consumption,
 * a maximum-attempts lockout and a resend cooldown. The plaintext OTP is only ever held
 * in memory long enough to hand it to EmailService - it is never persisted or logged
 * (MockEmailService's console logging is the one deliberate dev-only exception).
 */
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpVerificationRepository otpVerificationRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.otp.length:6}")
    private int otpLength;

    @Value("${app.otp.validity-minutes:10}")
    private long validityMinutes;

    @Value("${app.otp.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.otp.resend-cooldown-seconds:60}")
    private long resendCooldownSeconds;

    @Transactional
    public String generate(User user, OtpPurpose purpose) {
        otpVerificationRepository.findTopByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(user.getId(), purpose)
                .ifPresent(last -> {
                    Instant earliestAllowed = last.getCreatedAt().plusSeconds(resendCooldownSeconds);
                    if (Instant.now().isBefore(earliestAllowed)) {
                        long waitSeconds = Duration.between(Instant.now(), earliestAllowed).getSeconds() + 1;
                        throw new BadRequestException(
                                "Please wait " + waitSeconds + " seconds before requesting another code",
                                "OTP_RESEND_COOLDOWN");
                    }
                    // Invalidate the previous unconsumed OTP so only the newest one is ever valid.
                    last.setConsumed(true);
                    otpVerificationRepository.save(last);
                });

        String otp = generateNumericOtp();
        OtpVerification verification = OtpVerification.builder()
                .user(user)
                .purpose(purpose)
                .otpHash(HashUtil.sha256Hex(otp))
                .expiresAt(Instant.now().plus(Duration.ofMinutes(validityMinutes)))
                .attempts(0)
                .consumed(false)
                .build();
        otpVerificationRepository.save(verification);
        return otp;
    }

    @Transactional
    public void verify(User user, OtpPurpose purpose, String candidateOtp) {
        OtpVerification verification = otpVerificationRepository
                .findTopByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(user.getId(), purpose)
                .orElseThrow(() -> new BadRequestException("No active verification code found. Please request a new one.", "OTP_NOT_FOUND"));

        if (Instant.now().isAfter(verification.getExpiresAt())) {
            throw new BadRequestException("This code has expired. Please request a new one.", "OTP_EXPIRED");
        }
        if (verification.getAttempts() >= maxAttempts) {
            throw new BadRequestException("Maximum verification attempts exceeded. Please request a new code.", "OTP_MAX_ATTEMPTS");
        }

        verification.setAttempts(verification.getAttempts() + 1);

        if (candidateOtp == null || !constantTimeEquals(HashUtil.sha256Hex(candidateOtp), verification.getOtpHash())) {
            otpVerificationRepository.save(verification);
            int remaining = Math.max(0, maxAttempts - verification.getAttempts());
            throw new BadRequestException("Incorrect code. " + remaining + " attempt(s) remaining.", "OTP_INVALID");
        }

        verification.setConsumed(true);
        otpVerificationRepository.save(verification);
    }

    private String generateNumericOtp() {
        StringBuilder sb = new StringBuilder(otpLength);
        // First digit non-zero so the code always displays with the full requested length.
        sb.append(1 + secureRandom.nextInt(9));
        for (int i = 1; i < otpLength; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
