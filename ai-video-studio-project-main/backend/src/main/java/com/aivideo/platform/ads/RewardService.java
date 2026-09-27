package com.aivideo.platform.ads;

import com.aivideo.platform.ads.dto.AdVerifyRequest;
import com.aivideo.platform.ads.dto.RewardResponse;
import com.aivideo.platform.ads.dto.RewardStatusResponse;
import com.aivideo.platform.ads.entity.AdReward;
import com.aivideo.platform.ads.entity.DailyReward;
import com.aivideo.platform.ads.entity.RewardStatus;
import com.aivideo.platform.ads.provider.AdProvider;
import com.aivideo.platform.ads.provider.AdVerificationResult;
import com.aivideo.platform.common.exception.BadRequestException;
import com.aivideo.platform.common.exception.ResourceNotFoundException;
import com.aivideo.platform.user.User;
import com.aivideo.platform.user.UserRepository;
import com.aivideo.platform.wallet.WalletService;
import com.aivideo.platform.wallet.entity.TransactionType;
import com.aivideo.platform.wallet.entity.WalletTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Claim order matters here: the DailyReward row (guarded by a UNIQUE(user_id, reward_date)
 * DB constraint) is inserted and flushed BEFORE any diamonds are credited. If two requests
 * for the same user race past the initial "already claimed?" read, only one can win the
 * insert; the loser hits the unique-constraint violation and is turned into a normal
 * "already claimed" error having never touched the wallet. This mirrors, for the reward path,
 * the same "never credit twice" guarantee the payment webhook has for purchases.
 */
@Service
@RequiredArgsConstructor
public class RewardService {

    private final DailyRewardRepository dailyRewardRepository;
    private final AdRewardRepository adRewardRepository;
    private final AdProvider adProvider;
    private final WalletService walletService;
    private final UserRepository userRepository;

    @Value("${app.diamonds.daily-reward-amount:3}")
    private int dailyRewardAmount;

    @Transactional(readOnly = true)
    public RewardStatusResponse status(Long userId) {
        Optional<DailyReward> claimed = dailyRewardRepository.findByUserIdAndRewardDate(userId, LocalDate.now());
        return new RewardStatusResponse(claimed.isPresent(), dailyRewardAmount, claimed.map(DailyReward::getDiamonds).orElse(0));
    }

    @Transactional
    public RewardResponse claimAdReward(Long userId, AdVerifyRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (adRewardRepository.findByProviderAndVerificationId(request.provider(), request.verificationId()).isPresent()) {
            throw new BadRequestException("This ad verification has already been used", "AD_VERIFICATION_REUSED");
        }

        AdVerificationResult verification = adProvider.verifyReward(request.verificationId());
        if (!verification.verified()) {
            throw new BadRequestException("We couldn't verify that you watched the ad. Please try again.", "AD_VERIFICATION_FAILED");
        }

        LocalDate today = LocalDate.now();
        if (dailyRewardRepository.findByUserIdAndRewardDate(userId, today).isPresent()) {
            throw new BadRequestException("Today's reward has already been claimed. Come back tomorrow.", "DAILY_REWARD_CLAIMED");
        }

        try {
            dailyRewardRepository.saveAndFlush(DailyReward.builder()
                    .user(user)
                    .rewardDate(today)
                    .diamonds(dailyRewardAmount)
                    .status(RewardStatus.CREDITED)
                    .build());
        } catch (DataIntegrityViolationException e) {
            // A concurrent request for the same user+day won the race between our check above
            // and this insert. Nothing has been credited yet, so it's safe to simply reject.
            throw new BadRequestException("Today's reward has already been claimed. Come back tomorrow.", "DAILY_REWARD_CLAIMED");
        }

        adRewardRepository.save(AdReward.builder()
                .user(user)
                .provider(request.provider())
                .verificationId(request.verificationId())
                .rewardAmount(dailyRewardAmount)
                .status(RewardStatus.CREDITED)
                .rewardDate(today)
                .build());

        WalletTransaction transaction = walletService.credit(userId, dailyRewardAmount, TransactionType.AD_REWARD,
                "daily_reward:" + today, "Daily rewarded-ad bonus");

        return new RewardResponse(dailyRewardAmount, transaction.getBalanceAfter());
    }
}
