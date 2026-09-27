package com.aivideo.platform.ads;

import com.aivideo.platform.ads.dto.AdVerifyRequest;
import com.aivideo.platform.ads.dto.RewardResponse;
import com.aivideo.platform.common.exception.BadRequestException;
import com.aivideo.platform.user.User;
import com.aivideo.platform.user.UserRepository;
import com.aivideo.platform.user.UserRole;
import com.aivideo.platform.user.UserStatus;
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
class RewardServiceTest {

    @Autowired
    private RewardService rewardService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WalletRepository walletRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .name("Reward Test User")
                .email("reward-" + System.nanoTime() + "@example.com")
                .mobile("9123453333")
                .passwordHash("irrelevant")
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .build());
        walletRepository.save(Wallet.builder().user(user).diamondBalance(0L).build());
    }

    @Test
    void firstClaimOfTheDay_credits3Diamonds() {
        RewardResponse response = rewardService.claimAdReward(user.getId(),
                new AdVerifyRequest("MOCK_AD_NETWORK", "verification-" + System.nanoTime()));

        assertEquals(3, response.diamondsCredited());
        assertEquals(3L, walletRepository.findByUserId(user.getId()).orElseThrow().getDiamondBalance());
    }

    @Test
    void secondClaimOnTheSameDay_isRejected_andDoesNotCreditAgain() {
        rewardService.claimAdReward(user.getId(), new AdVerifyRequest("MOCK_AD_NETWORK", "verification-a-" + System.nanoTime()));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> rewardService.claimAdReward(
                user.getId(), new AdVerifyRequest("MOCK_AD_NETWORK", "verification-b-" + System.nanoTime())));
        assertEquals("DAILY_REWARD_CLAIMED", ex.getErrorCode());

        assertEquals(3L, walletRepository.findByUserId(user.getId()).orElseThrow().getDiamondBalance(),
                "balance must still reflect only the first claim");
    }

    @Test
    void reusingTheSameAdVerificationId_isRejected() {
        String verificationId = "verification-reuse-" + System.nanoTime();
        rewardService.claimAdReward(user.getId(), new AdVerifyRequest("MOCK_AD_NETWORK", verificationId));

        // Even ignoring the daily-claim limit, the exact same verification id must never be
        // usable twice (defends against replay of a captured verification token).
        assertThrows(BadRequestException.class, () ->
                rewardService.claimAdReward(user.getId(), new AdVerifyRequest("MOCK_AD_NETWORK", verificationId)));
    }
}
