package com.aivideo.platform.auth;

/**
 * Provider abstraction for outbound email. Swap MockEmailService for SmtpEmailService by
 * setting APP_MODE=live (see EmailServiceConfig) once MAIL_HOST/MAIL_USERNAME/MAIL_PASSWORD
 * are configured.
 */
public interface EmailService {

    void sendOtpEmail(String toEmail, String name, String otp, String purposeDescription);

    void sendPasswordChangedNotice(String toEmail, String name);
}
