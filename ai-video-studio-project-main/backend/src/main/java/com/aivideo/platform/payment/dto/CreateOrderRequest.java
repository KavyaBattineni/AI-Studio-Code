package com.aivideo.platform.payment.dto;

import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
        @NotNull(message = "Package ID is required")
        Long packageId
) {
}
