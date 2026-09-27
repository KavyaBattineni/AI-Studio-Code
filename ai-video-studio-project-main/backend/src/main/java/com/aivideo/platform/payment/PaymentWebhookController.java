package com.aivideo.platform.payment;

import com.aivideo.platform.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Server-to-server endpoint the payment gateway calls directly (not the browser). Kept as a
 * separate controller from PaymentController so it's obvious at a glance that this path is
 * public (see SecurityConfig) and authenticates the CALLER via the payload signature instead
 * of a user's JWT - there is no logged-in user on this request.
 *
 * Example signed request against the mock provider's default dev secret
 * (mock-dev-payment-secret-change-me - override via PAYMENT_SECRET):
 *
 *   BODY='{"orderId":"ORD-...","providerPaymentId":"mock_pay_123","status":"SUCCESS"}'
 *   SIG=$(echo -n "$BODY" | openssl dgst -sha256 -hmac "mock-dev-payment-secret-change-me" | sed 's/^.* //')
 *   curl -X POST localhost:8080/api/payments/webhook -H "Content-Type: application/json" \
 *        -H "X-Webhook-Signature: $SIG" -d "$BODY"
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments")
public class PaymentWebhookController {

    private final PaymentService paymentService;

    @PostMapping("/webhook")
    public ApiResponse<Void> webhook(@RequestBody String rawPayload,
                                      @RequestHeader(value = "X-Webhook-Signature", required = false) String signature) {
        paymentService.handleWebhook(rawPayload, signature);
        return ApiResponse.success("Webhook processed");
    }
}
