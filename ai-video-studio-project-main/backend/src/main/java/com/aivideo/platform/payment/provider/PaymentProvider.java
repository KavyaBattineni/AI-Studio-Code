package com.aivideo.platform.payment.provider;

import java.math.BigDecimal;

/**
 * Abstraction over the payment gateway. Implement this against a real gateway's actual,
 * currently-documented API for production (e.g. Razorpay, Stripe, PayU - whichever you
 * integrate) and select it via app.mode=live plus PAYMENT_KEY/PAYMENT_SECRET. This interface
 * intentionally mirrors the generic order -> verify -> webhook shape common to most gateways
 * rather than any one vendor's specific field names.
 */
public interface PaymentProvider {

    PaymentOrderResult createOrder(String internalOrderId, BigDecimal amount, String currency);

    PaymentVerificationResult verifyPayment(String internalOrderId, String providerPaymentId, String clientSignature);

    /** Validates an inbound webhook payload's signature against the shared secret. */
    boolean verifyWebhookSignature(String rawPayload, String signatureHeader);
}
