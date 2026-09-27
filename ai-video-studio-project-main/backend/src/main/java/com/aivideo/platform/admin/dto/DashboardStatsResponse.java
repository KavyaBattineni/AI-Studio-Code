package com.aivideo.platform.admin.dto;

import java.math.BigDecimal;

public record DashboardStatsResponse(
        long totalUsers,
        long activeUsers,
        long newUsersLast7Days,
        long totalVideos,
        long completedVideos,
        long failedVideos,
        long blockedModerationRequests,
        long diamondsPurchased,
        long diamondsUsed,
        long freeDiamondsDistributed,
        BigDecimal totalPaymentRevenue,
        long adRewardsClaimed
) {
}
