package com.aivideo.platform.video;

import com.aivideo.platform.video.entity.VideoJob;
import com.aivideo.platform.video.provider.VideoGenerationResult;
import com.aivideo.platform.video.provider.VideoProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for VideoJobCreatedEvent AFTER the transaction that created the job (and debited
 * diamonds) has committed, then drives the job to a terminal state on a background thread
 * pool (see AsyncConfig.videoTaskExecutor) - the original HTTP request already returned with
 * status QUEUED long before this runs, satisfying "do not block the request while waiting on
 * the video provider".
 *
 * This class deliberately holds no @Transactional methods of its own; it delegates every
 * state change to VideoService (a different Spring bean) so those methods go through the
 * normal transactional proxy instead of being skipped via same-class self-invocation.
 *
 * Swapping this for a message-queue-based pipeline (RabbitMQ/Kafka/SQS) later only means
 * replacing "how this method gets invoked" (a queue consumer instead of an event listener) -
 * the VideoService methods it calls, and therefore all the business logic, stay the same.
 */
@Service
@RequiredArgsConstructor
public class VideoProcessingService {

    private static final Logger log = LoggerFactory.getLogger(VideoProcessingService.class);

    private final VideoJobRepository videoJobRepository;
    private final VideoProvider videoProvider;
    private final VideoService videoService;

    @Async("videoTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVideoJobCreated(VideoJobCreatedEvent event) {
        process(event.jobId());
    }

    private void process(Long jobId) {
        VideoJob job = videoJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.warn("VideoJobCreatedEvent fired for missing job id={}", jobId);
            return;
        }

        videoService.markProcessing(jobId);

        try {
            VideoGenerationResult result = videoProvider.createVideo(job.getPrompt(), job.getDuration());
            if ("COMPLETED".equals(result.status())) {
                videoService.markCompleted(jobId, result.videoUrl(), result.thumbnailUrl());
            } else {
                videoService.markFailedAndRefund(jobId, safeMessage(result.errorMessage()));
            }
        } catch (Exception ex) {
            log.error("Video provider threw while processing job id={}", jobId, ex);
            videoService.markFailedAndRefund(jobId, "The video provider encountered an unexpected error.");
        }
    }

    private String safeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Video generation failed at the provider.";
        }
        return message.length() > 480 ? message.substring(0, 480) : message;
    }
}
