package com.aivideo.platform.auth;

import com.aivideo.platform.auth.dto.*;
import com.aivideo.platform.auth.entity.OtpPurpose;
import com.aivideo.platform.auth.entity.RefreshToken;
import com.aivideo.platform.common.HashUtil;
import com.aivideo.platform.common.exception.BadRequestException;
import com.aivideo.platform.common.exception.DuplicateResourceException;
import com.aivideo.platform.common.exception.ResourceNotFoundException;
import com.aivideo.platform.common.exception.UnauthorizedException;
import com.aivideo.platform.security.JwtService;
import com.aivideo.platform.security.SecurityUser;
import com.aivideo.platform.user.User;
import com.aivideo.platform.user.UserRepository;
import com.aivideo.platform.user.UserRole;
import com.aivideo.platform.user.UserStatus;
import com.aivideo.platform.user.dto.UserResponse;
import com.aivideo.platform.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpService otpService;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final WalletService walletService;

    @Transactional
    public void register(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new BadRequestException("Passwords do not match", "PASSWORD_MISMATCH");
        }
        String normalizedEmail = request.email().toLowerCase().trim();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        User user = User.builder()
                .name(request.name().trim())
                .email(normalizedEmail)
                .mobile(request.mobile().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .emailVerified(false)
                .status(UserStatus.PENDING_VERIFICATION)
                .role(UserRole.USER)
                .build();
        user = userRepository.save(user);
        walletService.getOrCreateWallet(user.getId());

        String otp = otpService.generate(user, OtpPurpose.REGISTRATION);
        emailService.sendOtpEmail(user.getEmail(), user.getName(), otp, "Verify your AI Video Studio account");
    }

    @Transactional
    public AuthResponse verifyOtpAndActivate(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(() -> new BadRequestException("Incorrect code.", "OTP_INVALID"));

        otpService.verify(user, OtpPurpose.REGISTRATION, request.otp());

        user.setEmailVerified(true);
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            user.setStatus(UserStatus.ACTIVE);
        }
        user = userRepository.save(user);

        return issueTokens(user);
    }

    @Transactional
    public void resendOtp(ResendOtpRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(() -> new BadRequestException("No pending registration found for this email"));
        if (user.isEmailVerified()) {
            throw new BadRequestException("This account is already verified. Please log in.");
        }
        String otp = otpService.generate(user, OtpPurpose.REGISTRATION);
        emailService.sendOtpEmail(user.getEmail(), user.getName(), otp, "Verify your AI Video Studio account");
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase().trim()).orElse(null);

        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        switch (user.getStatus()) {
            case PENDING_VERIFICATION ->
                    throw new UnauthorizedException("Please verify your email before logging in");
            case SUSPENDED ->
                    throw new UnauthorizedException("This account has been suspended. Contact support for help.");
            case INACTIVE ->
                    throw new UnauthorizedException("This account is inactive. Contact support for help.");
            case ACTIVE -> {
                // proceed
            }
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String tokenHash = HashUtil.sha256Hex(request.refreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));

        if (Instant.now().isAfter(stored.getExpiresAt())) {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        // Rotate: the presented refresh token is single-use.
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = userRepository.findById(stored.getUser().getId())
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("This account is no longer active");
        }

        return issueTokens(user);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Always behaves the same from the caller's perspective whether or not the email
        // exists - see AuthController for the single generic response returned either way.
        userRepository.findByEmail(request.email().toLowerCase().trim()).ifPresent(user -> {
            if (user.getStatus() != UserStatus.SUSPENDED) {
                String otp = otpService.generate(user, OtpPurpose.PASSWORD_RESET);
                emailService.sendOtpEmail(user.getEmail(), user.getName(), otp, "Reset your AI Video Studio password");
            }
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw new BadRequestException("Passwords do not match", "PASSWORD_MISMATCH");
        }
        User user = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(() -> new BadRequestException("Incorrect code.", "OTP_INVALID"));

        otpService.verify(user, OtpPurpose.PASSWORD_RESET, request.otp());

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokenRepository.revokeAllForUser(user.getId());
        emailService.sendPasswordChangedNotice(user.getEmail(), user.getName());
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw new BadRequestException("Passwords do not match", "PASSWORD_MISMATCH");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect", "INVALID_CURRENT_PASSWORD");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokenRepository.revokeAllForUser(user.getId());
        emailService.sendPasswordChangedNotice(user.getEmail(), user.getName());
    }

    private AuthResponse issueTokens(User user) {
        SecurityUser securityUser = new SecurityUser(user);
        String accessToken = jwtService.generateAccessToken(securityUser);
        String refreshTokenValue = jwtService.generateRefreshTokenValue();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(HashUtil.sha256Hex(refreshTokenValue))
                .expiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpirationMs()))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(accessToken, refreshTokenValue, UserResponse.from(user));
    }
}
