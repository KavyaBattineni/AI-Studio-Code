package com.aivideo.platform.video;

import com.aivideo.platform.common.exception.BadRequestException;
import com.aivideo.platform.common.exception.InsufficientDiamondsException;
import com.aivideo.platform.user.User;
import com.aivideo.platform.user.UserRepository;
import com.aivideo.platform.user.UserRole;
import com.aivideo.platform.user.UserStatus;
import com.aivideo.platform.video.dto.VideoGenerationRequest;
import com.aivideo.platform.video.dto.VideoJobResponse;
import com.aivideo.platform.video.entity.VideoJob;
import com.aivideo.platform.video.entity.VideoStatus;
import com.aivideo.platform.wallet.WalletRepository;
import com.aivideo.platform.wallet.entity.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class VideoServiceTest {

    @Autowired
    private VideoService videoService;
    @Autowired
    private VideoJobRepository videoJobRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WalletRepository walletRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .name("Video Test User")
                .email("video-" + System.nanoTime() + "@example.com")
                .mobile("9123451111")
                .passwordHash("irrelevant")
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .build());
        walletRepository.save(Wallet.builder().user(user).diamondBalance(20L).build());
    }

    @Test
    void fourSecondVideo_costsExactlyFiveDiamonds_serverSide() {
        VideoJobResponse job = videoService.generate(user.getId(), new VideoGenerationRequest("a calm sunset over the ocean", 4));
        assertEquals(5, job.diamondCost());
        assertEquals(15L, walletRepository.findByUserId(user.getId()).orElseThrow().getDiamondBalance());
    }

    @Test
    void eightSecondVideo_costsExactlyTenDiamonds_serverSide() {
        VideoJobResponse job = videoService.generate(user.getId(), new VideoGenerationRequest("a futuristic city at night", 8));
        assertEquals(10, job.diamondCost());
        assertEquals(10L, walletRepository.findByUserId(user.getId()).orElseThrow().getDiamondBalance());
    }

    @Test
    void unsupportedDuration_isRejected_andNoDiamondsAreTouched() {
        assertThrows(BadRequestException.class, () ->
                videoService.generate(user.getId(), new VideoGenerationRequest("a six second clip", 6)));
        assertEquals(20L, walletRepository.findByUserId(user.getId()).orElseThrow().getDiamondBalance());
    }

    @Test
    void insufficientBalance_isRejected_andDoesNotCreateAJobOrTouchTheWallet() {
        videoService.generate(user.getId(), new VideoGenerationRequest("prompt one", 8)); // 20 -> 10
        videoService.generate(user.getId(), new VideoGenerationRequest("prompt two", 8)); // 10 -> 0
        long jobsBefore = videoJobRepository.count();

        assertThrows(InsufficientDiamondsException.class, () ->
                videoService.generate(user.getId(), new VideoGenerationRequest("prompt three", 4))); // needs 5, has 0

        assertEquals(0L, walletRepository.findByUserId(user.getId()).orElseThrow().getDiamondBalance());
        assertEquals(jobsBefore, videoJobRepository.count(), "a failed generate() call must not leave an orphan job row");
    }

    @Test
    void successfulGeneration_eventuallyCompletesAsynchronously_withAVideoUrl() throws Exception {
        VideoJobResponse job = videoService.generate(user.getId(), new VideoGenerationRequest("a peaceful forest, birds singing", 4));

        VideoJob completed = awaitTerminalStatus(job.id(), Duration.ofSeconds(15));

        assertEquals(VideoStatus.COMPLETED, completed.getStatus());
        assertNotNull(completed.getVideoUrl());
        assertNotNull(completed.getThumbnailUrl());
    }

    @Test
    void failedGeneration_refundsDiamondsExactlyOnce() throws Exception {
        long balanceBefore = walletRepository.findByUserId(user.getId()).orElseThrow().getDiamondBalance();

        VideoJobResponse job = videoService.generate(user.getId(),
                new VideoGenerationRequest("this will FAIL_TEST on purpose", 4));
        assertEquals(balanceBefore - 5, walletRepository.findByUserId(user.getId()).orElseThrow().getDiamondBalance());

        VideoJob failed = awaitTerminalStatus(job.id(), Duration.ofSeconds(15));
        assertEquals(VideoStatus.FAILED, failed.getStatus());
        assertNotNull(failed.getErrorMessage());

        assertEquals(balanceBefore, walletRepository.findByUserId(user.getId()).orElseThrow().getDiamondBalance(),
                "diamonds should be fully refunded after a failed generation");
    }

    private VideoJob awaitTerminalStatus(Long jobId, Duration timeout) throws InterruptedException {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            VideoJob job = videoJobRepository.findById(jobId).orElseThrow();
            if (job.getStatus() == VideoStatus.COMPLETED || job.getStatus() == VideoStatus.FAILED) {
                return job;
            }
            Thread.sleep(200);
        }
        fail("video job " + jobId + " did not reach a terminal state within " + timeout);
        return null;
    }
}
