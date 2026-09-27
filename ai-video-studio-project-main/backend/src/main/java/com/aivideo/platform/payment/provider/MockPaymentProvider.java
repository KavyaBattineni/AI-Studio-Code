package com.aivideo.platform.payment.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Development/demo payment provider - no real gateway account required. Orders always
 * "succeed" on verify unless the client explicitly passes providerPaymentId="FAIL_TEST"
 * (used by the payment idempotency test and available for manual testing from Swagger).
 *
 * Webhook signatures ARE genuinely computed and checked here (HMAC-SHA256 over the raw
 * payload using PAYMENT_SECRET / app.providers.payment.secret as the shared key) - this is
 * the same mechanism a real gateway's webhook verification would use, just without a real
 * gateway on the other end. See PaymentServiceTest / the README for how to call the webhook
 * endpoint locally with a correctly signed payload.
 *
 * Swap this for a real gateway adapter (implementing PaymentProvider against that gateway's
 * actual documented API) selected via app.mode=live plus PAYMENT_KEY/PAYMENT_SECRET. Do not
 * invent a specific vendor's request/response shape - integrate against the real API you
 * actually have credentials for.
 */
@Component
@ConditionalOnProperty(name = "app.mode", havingValue = "mock", matchIfMissing = true)
public class MockPaymentProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentProvider.class);
    private static final String HMAC_ALGO = "HmacSHA256";

    @Value("${app.providers.payment.secret:mock-dev-payment-secret-change-me}")
    private String sharedSecret;

    @Override
    public PaymentOrderResult createOrder(String internalOrderId, BigDecimal amount, String currency) {
        String providerOrderId = "mock_order_" + UUID.randomUUID();
        log.info("[MOCK PAYMENT PROVIDER] Created order internalOrderId={} providerOrderId={} amount={} {}",
                internalOrderId, providerOrderId, amount, currency);
        return new PaymentOrderResult(providerOrderId, providerOrderId);
    }

    @Override
    public PaymentVerificationResult verifyPayment(String internalOrderId, String providerPaymentId, String clientSignature) {
        if ("FAIL_TEST".equals(providerPaymentId)) {
            return new PaymentVerificationResult(false, providerPaymentId, "Simulated payment failure (FAIL_TEST)");
        }
        String resolvedPaymentId = (providerPaymentId == null || providerPaymentId.isBlank())
                ? "mock_pay_" + UUID.randomUUID()
                : providerPaymentId;
        log.info("[MOCK PAYMENT PROVIDER] Verified payment order={} paymentId={}", internalOrderId, resolvedPaymentId);
        return new PaymentVerificationResult(true, resolvedPaymentId, "Payment verified");
    }

    @Override
    public boolean verifyWebhookSignature(String rawPayload, String signatureHeader) {
        if (rawPayload == null || signatureHeader == null) {
            return false;
        }
        String expected = hmacSha256Hex(rawPayload, sharedSecret);
        return constantTimeEquals(expected, signatureHeader.trim());
    }

    /** Exposed so callers building a test webhook payload (e.g. curl, the JUnit test) can sign it correctly. */
    public String sign(String rawPayload) {
        return hmacSha256Hex(rawPayload, sharedSecret);
    }

    private String hmacSha256Hex(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to compute webhook signature", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
