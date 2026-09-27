package com.aivideo.platform.admin.dto;

import java.math.BigDecimal;

public record RevenueResponse(
        BigDecimal applicationPaymentRevenue,
        long successfulPayments,
        long failedPayments,
        long refundedPayments,
        long diamondsPurchased,
        String note
) {
}
