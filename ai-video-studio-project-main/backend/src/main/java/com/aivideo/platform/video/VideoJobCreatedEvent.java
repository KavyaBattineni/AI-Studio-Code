package com.aivideo.platform.video;

/** Published after a VideoJob row + wallet debit commit; consumed by VideoProcessingService. */
public record VideoJobCreatedEvent(Long jobId) {
}
