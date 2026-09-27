package com.aivideo.platform.video;

import com.aivideo.platform.common.exception.BadRequestException;
import com.aivideo.platform.common.exception.ResourceNotFoundException;
import com.aivideo.platform.moderation.ModerationService;
import com.aivideo.platform.storage.StorageService;
import com.aivideo.platform.user.User;
import com.aivideo.platform.user.UserRepository;
import com.aivideo.platform.video.dto.VideoGenerationRequest;
import com.aivideo.platform.video.dto.VideoJobResponse;
import com.aivideo.platform.video.entity.VideoJob;
import com.aivideo.platform.video.entity.VideoStatus;
import com.aivideo.platform.wallet.WalletService;
import com.aivideo.platform.wallet.entity.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

/**
 * Orchestrates the flow described in the platform's video-generation transaction spec:
 * authenticate (done by Spring Security before we get here) -> validate -> moderate ->
 * price -> lock/check wallet -> debit -> create job -> (after commit) start provider job.
 *
 * The provider call itself never happens inside this class's transactional methods - it is
 * kicked off by VideoProcessingService only AFTER the debit+job-creation transaction commits
 * (see createAndQueueJob publishing VideoJobCreatedEvent), so we never hold the wallet's
 * pessimistic lock, or any other DB lock, while waiting on a potentially slow external call.
 */
@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoJobRepository videoJobRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final ModerationService moderationService;
    private final StorageService storageService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.diamonds.video-4s-cost:5}")
    private int cost4Seconds;

    @Value("${app.diamonds.video-8s-cost:10}")
    private int cost8Seconds;

    /** Server-authoritative pricing. The client sends only `duration`; cost is never trusted from React. */
    private int priceFor(int durationSeconds) {
        return switch (durationSeconds) {
            case 4 -> cost4Seconds;
            case 8 -> cost8Seconds;
            default -> throw new BadRequestException(
                    "Unsupported duration. Supported durations are 4 or 8 seconds.", "UNSUPPORTED_DURATION");
        };
    }

    @Transactional
    public VideoJobResponse generate(Long userId, VideoGenerationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String prompt = request.prompt().trim();
        int duration = request.duration();
        int cost = priceFor(duration); // validates duration as a side effect

        // Moderate BEFORE any diamonds are touched.
        moderationService.assertPromptAllowed(user, prompt, null);

        VideoJob job = VideoJob.builder()
                .user(user)
                .prompt(prompt)
                .duration(duration)
                .diamondCost(cost)
                .status(VideoStatus.QUEUED)
                .build();
        job = videoJobRepository.save(job);

        // Debit within the SAME transaction as job creation: if there aren't enough diamonds,
        // InsufficientDiamondsException rolls back the job insert too - atomic, no orphan jobs.
        walletService.debit(userId, cost, TransactionType.VIDEO_GENERATION, "video_job:" + job.getId(),
                "Video generation (" + duration + "s)");

        // Only after this method's transaction commits does anything call the provider.
        eventPublisher.publishEvent(new VideoJobCreatedEvent(job.getId()));

        return VideoJobResponse.from(job);
    }

    @Transactional(readOnly = true)
    public VideoJobResponse getJob(Long userId, Long jobId) {
        VideoJob job = videoJobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Video job not found"));
        return VideoJobResponse.from(job);
    }

    @Transactional(readOnly = true)
    public Page<VideoJobResponse> listJobs(Long userId, Pageable pageable) {
        return videoJobRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(VideoJobResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<com.aivideo.platform.video.dto.AdminVideoJobResponse> adminSearch(VideoStatus status, Long userId, Pageable pageable) {
        return videoJobRepository.adminSearch(status, userId, pageable).map(com.aivideo.platform.video.dto.AdminVideoJobResponse::from);
    }

    @Transactional
    public void deleteJob(Long userId, Long jobId) {
        VideoJob job = videoJobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Video job not found"));
        if (job.getStorageKey() != null) {
            storageService.deleteVideo(job.getStorageKey());
        }
        videoJobRepository.delete(job);
    }

    // ---------------------------------------------------------------------------------------
    // The methods below are called by VideoProcessingService (a different bean) from its
    // @Async background thread, each in its own short transaction. They are intentionally
    // NOT called from within this class (self-invocation would bypass Spring's @Transactional
    // proxy), and each one re-checks the job's current status before mutating it so a slow or
    // duplicated callback can never double-process a job that already reached a terminal state.
    // ---------------------------------------------------------------------------------------

    @Transactional
    public void markProcessing(Long jobId) {
        VideoJob job = videoJobRepository.findById(jobId).orElse(null);
        if (job == null || job.getStatus() != VideoStatus.QUEUED) {
            return;
        }
        job.setStatus(VideoStatus.PROCESSING);
        job.setStartedAt(Instant.now());
        videoJobRepository.save(job);
    }

    @Transactional
    public void markCompleted(Long jobId, String providerVideoUrl, String providerThumbnailUrl) {
        VideoJob job = videoJobRepository.findById(jobId).orElse(null);
        if (job == null || isTerminal(job.getStatus())) {
            return; // idempotent guard - never double-complete
        }
        String storageKey = "videos/" + job.getId() + ".mp4";
        String finalVideoUrl = storageService.uploadVideo(storageKey, new byte[0], "video/mp4");
        String finalThumbnailUrl = storageService.uploadThumbnail("thumbnails/" + job.getId() + ".jpg", new byte[0], "image/jpeg");

        job.setStatus(VideoStatus.COMPLETED);
        job.setStorageKey(storageKey);
        job.setVideoUrl(providerVideoUrl != null ? providerVideoUrl : finalVideoUrl);
        job.setThumbnailUrl(providerThumbnailUrl != null ? providerThumbnailUrl : finalThumbnailUrl);
        job.setCompletedAt(Instant.now());
        videoJobRepository.save(job);
    }

    @Transactional
    public void markFailedAndRefund(Long jobId, String errorMessage) {
        VideoJob job = videoJobRepository.findById(jobId).orElse(null);
        if (job == null || isTerminal(job.getStatus())) {
            return; // idempotent guard - never double-refund
        }
        job.setStatus(VideoStatus.FAILED);
        job.setErrorMessage(errorMessage);
        job.setCompletedAt(Instant.now());
        videoJobRepository.save(job);

        String referenceId = "video_job:" + job.getId();
        if (!walletService.hasTransaction(referenceId, TransactionType.REFUND)) {
            walletService.credit(job.getUser().getId(), job.getDiamondCost(), TransactionType.REFUND,
                    referenceId, "Refund for failed video generation");
        }
    }

    private boolean isTerminal(VideoStatus status) {
        return status == VideoStatus.COMPLETED || status == VideoStatus.FAILED || status == VideoStatus.CANCELLED;
    }
}
