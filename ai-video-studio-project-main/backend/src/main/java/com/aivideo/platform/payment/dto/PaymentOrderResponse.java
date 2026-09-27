package com.aivideo.platform.payment.dto;

import java.math.BigDecimal;

public record PaymentOrderResponse(
        String orderId,
        String providerOrderId,
        BigDecimal amount,
        String currency,
        int diamonds
) {
}
