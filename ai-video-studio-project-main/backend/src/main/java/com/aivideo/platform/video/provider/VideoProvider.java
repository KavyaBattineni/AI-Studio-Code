package com.aivideo.platform.video.provider;

/**
 * Abstraction over the actual AI video generation backend. Implement this against a real
 * provider's API for production (do not call an external API directly from VideoService -
 * always go through this interface) and select it via app.mode=live plus AI_VIDEO_API_KEY.
 */
public interface VideoProvider {

    /**
     * Synchronously drives one generation job to a terminal state (COMPLETED/FAILED) and
     * returns the result. Callers must invoke this off the HTTP request thread (see
     * VideoProcessingService) - a real provider implementation may take anywhere from
     * seconds to minutes.
     */
    VideoGenerationResult createVideo(String prompt, int durationSeconds);

    VideoGenerationResult getVideoStatus(String providerJobId);

    void cancelVideo(String providerJobId);
}
