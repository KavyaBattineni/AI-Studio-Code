package com.aivideo.platform.moderation.dto;

import com.aivideo.platform.moderation.entity.ModerationResult;

import java.time.Instant;

public record AdminModerationResponse(
        Long id,
        Long userId,
        String userEmail,
        String inputType,
        String inputSnippet,
        String decision,
        Instant createdAt
) {
    public static AdminModerationResponse from(ModerationResult m) {
        return new AdminModerationResponse(
                m.getId(), m.getUser().getId(), m.getUser().getEmail(),
                m.getInputType().name(), m.getInputSnippet(), m.getDecision().name(), m.getCreatedAt()
        );
    }
}
