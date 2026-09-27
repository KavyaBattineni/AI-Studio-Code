package com.aivideo.platform.auth;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Production email provider, backed by Spring Mail / SMTP. Activated when app.mode=live.
 * Configure MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD and MAIL_FROM in your
 * environment - this class never logs OTP values.
 */
@Service
@ConditionalOnProperty(name = "app.mode", havingValue = "live")
@RequiredArgsConstructor
public class SmtpEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Override
    public void sendOtpEmail(String toEmail, String name, String otp, String purposeDescription) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("AI Video Studio - Your verification code");
        message.setText("Hi " + name + ",\n\n" + purposeDescription + ". Your one-time code is: " + otp
                + "\n\nThis code expires shortly and can only be used once. If you did not request this, "
                + "you can safely ignore this email.\n\n- AI Video Studio");
        try {
            mailSender.send(message);
        } catch (Exception ex) {
            // Never rethrow raw SMTP errors to the caller/client; log for ops and let the
            // caller surface a generic failure message instead.
            log.error("Failed to send OTP email via SMTP", ex);
            throw new IllegalStateException("Unable to send verification email at this time");
        }
    }

    @Override
    public void sendPasswordChangedNotice(String toEmail, String name) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("AI Video Studio - Your password was changed");
        message.setText("Hi " + name + ",\n\nThis is a confirmation that your account password was just changed. "
                + "If this wasn't you, please reset your password immediately.\n\n- AI Video Studio");
        try {
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Failed to send password-changed email via SMTP", ex);
        }
    }
}
