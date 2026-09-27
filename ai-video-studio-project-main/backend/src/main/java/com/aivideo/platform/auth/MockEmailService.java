package com.aivideo.platform.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Development-only email provider. Instead of sending real email, it logs the OTP to the
 * console so the full registration/login/reset flow can be exercised without an SMTP account.
 *
 * ⚠️ Never enable this in production - it is only wired in when app.mode=mock (the default).
 * Logging an OTP value anywhere in production would defeat its purpose.
 */
@Service
@ConditionalOnProperty(name = "app.mode", havingValue = "mock", matchIfMissing = true)
public class MockEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(MockEmailService.class);

    @Override
    public void sendOtpEmail(String toEmail, String name, String otp, String purposeDescription) {
        log.warn("=================================================================");
        log.warn(" [DEV-ONLY MOCK EMAIL] {}", purposeDescription);
        log.warn(" To     : {} ({})", toEmail, name);
        log.warn(" OTP    : {}", otp);
        log.warn(" This OTP is only logged because APP_MODE=mock. Never log OTPs in production.");
        log.warn("=================================================================");
    }

    @Override
    public void sendPasswordChangedNotice(String toEmail, String name) {
        log.info("[DEV-ONLY MOCK EMAIL] Password changed notice sent to {} ({})", toEmail, name);
    }
}
