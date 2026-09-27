package com.aivideo.platform.moderation;

import com.aivideo.platform.common.exception.ModerationBlockedException;
import com.aivideo.platform.user.User;
import com.aivideo.platform.user.UserRepository;
import com.aivideo.platform.user.UserRole;
import com.aivideo.platform.user.UserStatus;
import com.aivideo.platform.video.VideoService;
import com.aivideo.platform.video.dto.VideoGenerationRequest;
import com.aivideo.platform.wallet.WalletRepository;
import com.aivideo.platform.wallet.entity.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ModerationServiceTest {

    @Autowired
    private ModerationService moderationService;
    @Autowired
    private VideoService videoService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WalletRepository walletRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .name("Moderation Test User")
                .email("moderation-" + System.nanoTime() + "@example.com")
                .mobile("9123452222")
                .passwordHash("irrelevant")
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .build());
        walletRepository.save(Wallet.builder().user(user).diamondBalance(20L).build());
    }

    @Test
    void safePrompt_isAllowed() {
        assertDoesNotThrow(() -> moderationService.assertPromptAllowed(user, "a golden retriever playing in a park", null));
    }

    @Test
    void disallowedPrompt_isBlocked_withAGenericMessage() {
        ModerationBlockedException ex = assertThrows(ModerationBlockedException.class, () ->
                moderationService.assertPromptAllowed(user, "generate explicit sexual content", null));
        assertEquals("This request isn't allowed. Please try a different prompt.", ex.getMessage());
    }

    @Test
    void blockedPromptSubmittedToVideoGeneration_neverTouchesTheWallet() {
        long balanceBefore = walletRepository.findByUserId(user.getId()).orElseThrow().getDiamondBalance();

        assertThrows(ModerationBlockedException.class, () -> videoService.generate(user.getId(),
                new VideoGenerationRequest("explicit sexual content of any kind", 4)));

        assertEquals(balanceBefore, walletRepository.findByUserId(user.getId()).orElseThrow().getDiamondBalance(),
                "moderation must run and block before any diamonds are ever touched");
    }
}
