package com.aivideo.platform.ads.dto;

import jakarta.validation.constraints.NotBlank;

public record AdVerifyRequest(
        @NotBlank(message = "Provider is required")
        String provider,

        @NotBlank(message = "Verification ID is required")
        String verificationId
) {
}
