package com.aivideo.platform.payment.dto;

import com.aivideo.platform.payment.entity.Payment;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminPaymentResponse(
        Long id,
        String orderId,
        Long userId,
        String userEmail,
        String packageName,
        BigDecimal amount,
        String currency,
        String status,
        Instant createdAt
) {
    public static AdminPaymentResponse from(Payment p) {
        return new AdminPaymentResponse(
                p.getId(), p.getOrderId(), p.getUser().getId(), p.getUser().getEmail(),
                p.getDiamondPackage().getName(), p.getAmount(), p.getCurrency(), p.getStatus().name(), p.getCreatedAt()
        );
    }
}
