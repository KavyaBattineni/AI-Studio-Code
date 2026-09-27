package com.aivideo.platform.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email,

        @NotBlank(message = "OTP is required")
        @Pattern(regexp = "^[0-9]{4,8}$", message = "OTP must be numeric")
        String otp
) {
}
