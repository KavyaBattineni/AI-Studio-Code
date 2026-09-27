package com.aivideo.platform.video.dto;

import com.aivideo.platform.video.entity.VideoJob;

import java.time.Instant;

public record AdminVideoJobResponse(
        Long id,
        Long userId,
        String userEmail,
        String prompt,
        int duration,
        int diamondCost,
        String status,
        Instant createdAt
) {
    public static AdminVideoJobResponse from(VideoJob job) {
        return new AdminVideoJobResponse(
                job.getId(), job.getUser().getId(), job.getUser().getEmail(), job.getPrompt(),
                job.getDuration(), job.getDiamondCost(), job.getStatus().name(), job.getCreatedAt()
        );
    }
}
