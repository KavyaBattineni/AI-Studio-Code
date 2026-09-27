package com.aivideo.platform.ads.dto;

public record RewardStatusResponse(
        boolean claimedToday,
        int diamondsAvailableToday,
        int diamondsClaimedToday
) {
}
