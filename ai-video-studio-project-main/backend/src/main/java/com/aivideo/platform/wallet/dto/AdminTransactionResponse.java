package com.aivideo.platform.wallet.dto;

import com.aivideo.platform.wallet.entity.WalletTransaction;

import java.time.Instant;

public record AdminTransactionResponse(
        Long id,
        Long userId,
        String userEmail,
        String type,
        long amount,
        long balanceBefore,
        long balanceAfter,
        String referenceId,
        String description,
        Instant createdAt
) {
    public static AdminTransactionResponse from(WalletTransaction t) {
        return new AdminTransactionResponse(
                t.getId(), t.getUser().getId(), t.getUser().getEmail(), t.getType().name(),
                t.getAmount(), t.getBalanceBefore(), t.getBalanceAfter(),
                t.getReferenceId(), t.getDescription(), t.getCreatedAt()
        );
    }
}
