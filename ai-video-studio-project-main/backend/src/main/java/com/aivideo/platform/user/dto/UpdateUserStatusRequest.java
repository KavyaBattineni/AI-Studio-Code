package com.aivideo.platform.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserStatusRequest(
        @NotBlank(message = "Status is required")
        String status
) {
}
