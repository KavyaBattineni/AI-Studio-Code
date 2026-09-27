package com.aivideo.platform.user.dto;

import com.aivideo.platform.user.User;

import java.time.Instant;

public record UserResponse(
        Long id,
        String name,
        String email,
        String mobile,
        boolean emailVerified,
        String status,
        String role,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getMobile(),
                user.isEmailVerified(),
                user.getStatus().name(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }
}
