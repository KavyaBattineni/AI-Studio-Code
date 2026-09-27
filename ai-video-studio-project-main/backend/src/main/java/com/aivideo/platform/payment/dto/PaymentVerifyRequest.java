package com.aivideo.platform.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentVerifyRequest(
        @NotBlank(message = "Order ID is required")
        String orderId,

        /** In mock mode this may be omitted; the mock provider will generate one. Pass the
         *  literal value "FAIL_TEST" to exercise the failure path end-to-end. */
        String providerPaymentId,

        String signature
) {
}
