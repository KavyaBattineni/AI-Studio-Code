package com.aivideo.platform.admin;

import com.aivideo.platform.admin.dto.DashboardStatsResponse;
import com.aivideo.platform.admin.dto.RevenueResponse;
import com.aivideo.platform.ads.AdRewardRepository;
import com.aivideo.platform.moderation.ModerationService;
import com.aivideo.platform.payment.PaymentRepository;
import com.aivideo.platform.payment.PaymentService;
import com.aivideo.platform.payment.entity.PaymentStatus;
import com.aivideo.platform.user.UserRepository;
import com.aivideo.platform.user.UserStatus;
import com.aivideo.platform.video.VideoJobRepository;
import com.aivideo.platform.video.entity.VideoStatus;
import com.aivideo.platform.wallet.WalletService;
import com.aivideo.platform.wallet.entity.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * This is the one service in the platform that is deliberately cross-cutting - a dashboard
 * necessarily summarizes every other module. Everywhere else, AdminController talks directly
 * to the owning module's service (UserService, PaymentService, VideoService, ModerationService,
 * WalletService) for its admin-facing list endpoints, so this class stays small and never
 * becomes a dumping ground for business logic that belongs elsewhere.
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final VideoJobRepository videoJobRepository;
    private final PaymentRepository paymentRepository;
    private final AdRewardRepository adRewardRepository;
    private final ModerationService moderationService;
    private final WalletService walletService;
    private final PaymentService paymentService;

    @Transactional(readOnly = true)
    public DashboardStatsResponse dashboard() {
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);

        long diamondsPurchased = walletService.sumAmountByType(TransactionType.PURCHASE);
        long diamondsUsed = Math.abs(walletService.sumAmountByType(TransactionType.VIDEO_GENERATION));
        long freeDiamonds = walletService.sumAmountByType(TransactionType.DAILY_REWARD)
                + walletService.sumAmountByType(TransactionType.AD_REWARD);

        return new DashboardStatsResponse(
                userRepository.count(),
                userRepository.countByStatus(UserStatus.ACTIVE),
                userRepository.countByCreatedAtAfter(sevenDaysAgo),
                videoJobRepository.count(),
                videoJobRepository.countByStatus(VideoStatus.COMPLETED),
                videoJobRepository.countByStatus(VideoStatus.FAILED),
                moderationService.countBlocked(),
                diamondsPurchased,
                diamondsUsed,
                freeDiamonds,
                paymentService.sumSuccessfulPayments(),
                adRewardRepository.count()
        );
    }

    @Transactional(readOnly = true)
    public RevenueResponse revenue() {
        BigDecimal applicationRevenue = paymentService.sumSuccessfulPayments();
        long successful = paymentRepository.countByStatus(PaymentStatus.SUCCESS);
        long failed = paymentRepository.countByStatus(PaymentStatus.FAILED);
        long refunded = paymentRepository.countByStatus(PaymentStatus.REFUNDED);
        long diamondsPurchased = walletService.sumAmountByType(TransactionType.PURCHASE);

        return new RevenueResponse(
                applicationRevenue,
                successful,
                failed,
                refunded,
                diamondsPurchased,
                "This figure is application payment revenue only (diamond purchases through this "
                        + "platform). It does not include ad-network publisher revenue, which the ad "
                        + "network calculates and reports separately according to its own policies."
        );
    }
}
