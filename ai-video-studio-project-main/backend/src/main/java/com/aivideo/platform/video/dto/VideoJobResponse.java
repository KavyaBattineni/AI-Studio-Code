package com.aivideo.platform.video.dto;

import com.aivideo.platform.video.entity.VideoJob;

import java.time.Instant;

public record VideoJobResponse(
        Long id,
        String prompt,
        int duration,
        int diamondCost,
        String status,
        String videoUrl,
        String thumbnailUrl,
        String errorMessage,
        Instant createdAt,
        Instant completedAt
) {
    public static VideoJobResponse from(VideoJob job) {
        return new VideoJobResponse(
                job.getId(), job.getPrompt(), job.getDuration(), job.getDiamondCost(),
                job.getStatus().name(), job.getVideoUrl(), job.getThumbnailUrl(),
                job.getErrorMessage(), job.getCreatedAt(), job.getCompletedAt()
        );
    }
}
