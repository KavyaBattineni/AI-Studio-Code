package com.aivideo.platform.user.dto;

import com.aivideo.platform.user.User;

import java.time.Instant;

/** Same as UserResponse but includes the diamond balance, for the admin users table. */
public record AdminUserResponse(
        Long id,
        String name,
        String email,
        String mobile,
        String status,
        String role,
        long diamondBalance,
        Instant createdAt
) {
    public static AdminUserResponse from(User user, long diamondBalance) {
        return new AdminUserResponse(
                user.getId(), user.getName(), user.getEmail(), user.getMobile(),
                user.getStatus().name(), user.getRole().name(), diamondBalance, user.getCreatedAt()
        );
    }
}
