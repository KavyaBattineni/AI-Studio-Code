package com.aivideo.platform.payment.provider;

public record PaymentVerificationResult(boolean success, String providerPaymentId, String message) {
}
