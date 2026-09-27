package com.aivideo.platform.payment.dto;

import com.aivideo.platform.payment.entity.Payment;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        String orderId,
        String paymentId,
        BigDecimal amount,
        String currency,
        String gateway,
        String status,
        int diamonds,
        Instant createdAt,
        Instant completedAt
) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(
                p.getId(), p.getOrderId(), p.getPaymentId(), p.getAmount(), p.getCurrency(),
                p.getGateway(), p.getStatus().name(), p.getDiamonds(), p.getCreatedAt(), p.getCompletedAt()
        );
    }
}
