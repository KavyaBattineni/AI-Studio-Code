package com.aivideo.platform.auth.dto;

import com.aivideo.platform.user.dto.UserResponse;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserResponse user
) {
}
