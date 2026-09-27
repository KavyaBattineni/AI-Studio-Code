package com.aivideo.platform.auth;

import com.aivideo.platform.auth.entity.OtpPurpose;
import com.aivideo.platform.common.exception.BadRequestException;
import com.aivideo.platform.user.User;
import com.aivideo.platform.user.UserRepository;
import com.aivideo.platform.user.UserRole;
import com.aivideo.platform.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class OtpServiceTest {

    @Autowired
    private OtpService otpService;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .name("OTP Test User")
                .email("otp-" + System.nanoTime() + "@example.com")
                .mobile("9000000002")
                .passwordHash("irrelevant")
                .emailVerified(false)
                .status(UserStatus.PENDING_VERIFICATION)
                .role(UserRole.USER)
                .build());
    }

    @Test
    void correctOtp_verifiesSuccessfully() {
        String otp = otpService.generate(user, OtpPurpose.REGISTRATION);
        assertEquals(6, otp.length());
        assertDoesNotThrow(() -> otpService.verify(user, OtpPurpose.REGISTRATION, otp));
    }

    @Test
    void otpIsSingleUse() {
        String otp = otpService.generate(user, OtpPurpose.REGISTRATION);
        otpService.verify(user, OtpPurpose.REGISTRATION, otp);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> otpService.verify(user, OtpPurpose.REGISTRATION, otp));
        assertEquals("OTP_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    void wrongOtp_isRejectedAndDoesNotConsumeTheRealOtp() {
        String otp = otpService.generate(user, OtpPurpose.REGISTRATION);

        assertThrows(BadRequestException.class, () -> otpService.verify(user, OtpPurpose.REGISTRATION, "000000"));
        // the real code should still work after one wrong attempt
        assertDoesNotThrow(() -> otpService.verify(user, OtpPurpose.REGISTRATION, otp));
    }

    @Test
    void tooManyWrongAttempts_locksOutTheCode() {
        String otp = otpService.generate(user, OtpPurpose.REGISTRATION);
        String wrong = "000000".equals(otp) ? "111111" : "000000";

        for (int i = 0; i < 5; i++) {
            assertThrows(BadRequestException.class, () -> otpService.verify(user, OtpPurpose.REGISTRATION, wrong));
        }

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> otpService.verify(user, OtpPurpose.REGISTRATION, otp));
        assertEquals("OTP_MAX_ATTEMPTS", ex.getErrorCode());
    }

    @Test
    void resendBeforeCooldownElapses_isRejected() {
        otpService.generate(user, OtpPurpose.REGISTRATION);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> otpService.generate(user, OtpPurpose.REGISTRATION));
        assertEquals("OTP_RESEND_COOLDOWN", ex.getErrorCode());
    }
}
