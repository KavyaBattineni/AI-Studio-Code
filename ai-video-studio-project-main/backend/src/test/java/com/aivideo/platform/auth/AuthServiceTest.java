package com.aivideo.platform.auth;

import com.aivideo.platform.auth.dto.RegisterRequest;
import com.aivideo.platform.common.exception.BadRequestException;
import com.aivideo.platform.common.exception.DuplicateResourceException;
import com.aivideo.platform.user.User;
import com.aivideo.platform.user.UserRepository;
import com.aivideo.platform.wallet.WalletRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Test
    void register_createsPendingUserWithAWallet_andDoesNotStorePlaintextPassword() {
        String email = "register-" + System.nanoTime() + "@example.com";
        authService.register(new RegisterRequest("Jane Doe", email, "9123456780", "Password@123", "Password@123"));

        User saved = userRepository.findByEmail(email).orElseThrow();
        assertEquals("PENDING_VERIFICATION", saved.getStatus().name());
        assertFalse(saved.isEmailVerified());
        assertNotEquals("Password@123", saved.getPasswordHash(), "password must never be stored in plaintext");
        assertTrue(saved.getPasswordHash().startsWith("$2"), "password should be BCrypt-hashed");

        assertTrue(walletRepository.findByUserId(saved.getId()).isPresent(), "every user must get a wallet");
    }

    @Test
    void register_rejectsMismatchedPasswords() {
        String email = "mismatch-" + System.nanoTime() + "@example.com";
        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.register(
                new RegisterRequest("Jane Doe", email, "9123456780", "Password@123", "Different@123")));
        assertEquals("PASSWORD_MISMATCH", ex.getErrorCode());
        assertTrue(userRepository.findByEmail(email).isEmpty());
    }

    @Test
    void register_rejectsDuplicateEmail() {
        String email = "duplicate-" + System.nanoTime() + "@example.com";
        authService.register(new RegisterRequest("First", email, "9123456780", "Password@123", "Password@123"));

        assertThrows(DuplicateResourceException.class, () -> authService.register(
                new RegisterRequest("Second", email, "9123456781", "Password@123", "Password@123")));
    }
}
