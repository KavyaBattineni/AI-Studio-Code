package com.aivideo.platform.payment;

import com.aivideo.platform.common.exception.BadRequestException;
import com.aivideo.platform.common.exception.PaymentException;
import com.aivideo.platform.common.exception.ResourceNotFoundException;
import com.aivideo.platform.diamond.DiamondService;
import com.aivideo.platform.diamond.entity.DiamondPackage;
import com.aivideo.platform.payment.dto.*;
import com.aivideo.platform.payment.entity.Payment;
import com.aivideo.platform.payment.entity.PaymentStatus;
import com.aivideo.platform.payment.provider.PaymentOrderResult;
import com.aivideo.platform.payment.provider.PaymentProvider;
import com.aivideo.platform.payment.provider.PaymentVerificationResult;
import com.aivideo.platform.user.User;
import com.aivideo.platform.user.UserRepository;
import com.aivideo.platform.wallet.WalletService;
import com.aivideo.platform.wallet.entity.TransactionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Diamonds are only ever credited from two places in this class: a successful verify() call
 * or a successful handleWebhook() call - never because the frontend merely says "payment
 * successful". Both paths lock the Payment row (PESSIMISTIC_WRITE) and check its current
 * status before crediting, so a retried verify call, a duplicate webhook delivery, or both
 * arriving at nearly the same moment can only ever result in one PURCHASE wallet transaction
 * per order (see PaymentRepository.findByOrderIdForUpdate).
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final DiamondService diamondService;
    private final PaymentProvider paymentProvider;
    private final WalletService walletService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private record WebhookPayload(String orderId, String providerPaymentId, String status) {
    }

    @Transactional
    public PaymentOrderResponse createOrder(Long userId, CreateOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        DiamondPackage pkg = diamondService.getActivePackage(request.packageId());

        String orderId = "ORD-" + UUID.randomUUID();
        PaymentOrderResult providerResult = paymentProvider.createOrder(orderId, pkg.getPrice(), pkg.getCurrency());

        Payment payment = Payment.builder()
                .user(user)
                .diamondPackage(pkg)
                .orderId(orderId)
                .amount(pkg.getPrice())
                .currency(pkg.getCurrency())
                .gateway("MOCK")
                .status(PaymentStatus.CREATED)
                .diamonds(pkg.getDiamonds())
                .build();
        paymentRepository.save(payment);

        return new PaymentOrderResponse(orderId, providerResult.providerOrderId(), pkg.getPrice(), pkg.getCurrency(), pkg.getDiamonds());
    }

    @Transactional
    public PaymentResponse verify(Long userId, PaymentVerifyRequest request) {
        Payment payment = paymentRepository.findByOrderIdForUpdate(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!payment.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Order not found");
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return PaymentResponse.from(payment); // already credited - idempotent no-op
        }
        if (payment.getStatus() == PaymentStatus.REFUNDED || payment.getStatus() == PaymentStatus.FAILED) {
            throw new PaymentException("This order can no longer be verified");
        }

        PaymentVerificationResult result =
                paymentProvider.verifyPayment(payment.getOrderId(), request.providerPaymentId(), request.signature());

        if (!result.success()) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setPaymentId(result.providerPaymentId());
            paymentRepository.save(payment);
            throw new PaymentException("Payment verification failed: " + result.message());
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentId(result.providerPaymentId());
        payment.setCompletedAt(Instant.now());
        paymentRepository.save(payment);

        walletService.credit(userId, payment.getDiamonds(), TransactionType.PURCHASE,
                "payment:" + payment.getOrderId(), "Diamond purchase - " + payment.getDiamondPackage().getName());

        return PaymentResponse.from(payment);
    }

    @Transactional
    public void handleWebhook(String rawPayload, String signatureHeader) {
        if (!paymentProvider.verifyWebhookSignature(rawPayload, signatureHeader)) {
            throw new PaymentException("Invalid webhook signature");
        }

        WebhookPayload payload;
        try {
            payload = objectMapper.readValue(rawPayload, WebhookPayload.class);
        } catch (Exception e) {
            throw new BadRequestException("Malformed webhook payload");
        }

        Payment payment = paymentRepository.findByOrderIdForUpdate(payload.orderId()).orElse(null);
        if (payment == null) {
            log.warn("Webhook received for unknown order id={}", payload.orderId());
            return; // 200 OK regardless - an unknown order isn't something the gateway should keep retrying
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS || payment.getStatus() == PaymentStatus.FAILED) {
            log.info("Ignoring duplicate webhook for order {} (already {})", payment.getOrderId(), payment.getStatus());
            return; // idempotent - duplicate delivery
        }

        if ("SUCCESS".equalsIgnoreCase(payload.status())) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaymentId(payload.providerPaymentId());
            payment.setCompletedAt(Instant.now());
            paymentRepository.save(payment);

            walletService.credit(payment.getUser().getId(), payment.getDiamonds(), TransactionType.PURCHASE,
                    "payment:" + payment.getOrderId(), "Diamond purchase - " + payment.getDiamondPackage().getName());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setPaymentId(payload.providerPaymentId());
            paymentRepository.save(payment);
        }
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> history(Long userId, Pageable pageable) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(PaymentResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<com.aivideo.platform.payment.dto.AdminPaymentResponse> adminSearch(PaymentStatus status, Pageable pageable) {
        return paymentRepository.adminSearch(status, pageable).map(com.aivideo.platform.payment.dto.AdminPaymentResponse::from);
    }

    @Transactional(readOnly = true)
    public java.math.BigDecimal sumSuccessfulPayments() {
        return paymentRepository.sumSuccessfulAmount();
    }
}
