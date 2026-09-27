package com.aivideo.platform.wallet.dto;

import com.aivideo.platform.wallet.entity.WalletTransaction;

import java.time.Instant;

public record TransactionResponse(
        Long id,
        String type,
        long amount,
        long balanceBefore,
        long balanceAfter,
        String referenceId,
        String description,
        Instant createdAt
) {
    public static TransactionResponse from(WalletTransaction t) {
        return new TransactionResponse(
                t.getId(), t.getType().name(), t.getAmount(), t.getBalanceBefore(),
                t.getBalanceAfter(), t.getReferenceId(), t.getDescription(), t.getCreatedAt()
        );
    }
}
