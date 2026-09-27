package com.aivideo.platform.video.provider;

/**
 * status is one of QUEUED / PROCESSING / COMPLETED / FAILED (mirrors VideoStatus, kept as a
 * plain string here so the provider package has zero dependency on the persistence layer).
 */
public record VideoGenerationResult(
        String providerJobId,
        String status,
        String videoUrl,
        String thumbnailUrl,
        String errorMessage
) {
    public static VideoGenerationResult completed(String providerJobId, String videoUrl, String thumbnailUrl) {
        return new VideoGenerationResult(providerJobId, "COMPLETED", videoUrl, thumbnailUrl, null);
    }

    public static VideoGenerationResult failed(String providerJobId, String errorMessage) {
        return new VideoGenerationResult(providerJobId, "FAILED", null, null, errorMessage);
    }
}
