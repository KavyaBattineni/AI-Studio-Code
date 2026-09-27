package com.aivideo.platform.payment;

import com.aivideo.platform.diamond.entity.DiamondPackage;
import com.aivideo.platform.diamond.DiamondPackageRepository;
import com.aivideo.platform.payment.dto.CreateOrderRequest;
import com.aivideo.platform.payment.dto.PaymentOrderResponse;
import com.aivideo.platform.payment.dto.PaymentResponse;
import com.aivideo.platform.payment.dto.PaymentVerifyRequest;
import com.aivideo.platform.payment.provider.MockPaymentProvider;
import com.aivideo.platform.user.User;
import com.aivideo.platform.user.UserRepository;
import com.aivideo.platform.user.UserRole;
import com.aivideo.platform.user.UserStatus;
import com.aivideo.platform.wallet.WalletRepository;
import com.aivideo.platform.wallet.WalletService;
import com.aivideo.platform.wallet.entity.Wallet;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class PaymentServiceIdempotencyTest {

    @Autowired
    private PaymentService paymentService;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private DiamondPackageRepository diamondPackageRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private WalletService walletService;
    @Autowired
    private MockPaymentProvider mockPaymentProvider;
    @Autowired
    private ObjectMapper objectMapper;

    private User user;
    private DiamondPackage pkg;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .name("Payment Test User")
                .email("payment-" + System.nanoTime() + "@example.com")
                .mobile("9123450000")
                .passwordHash("irrelevant")
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .build());
        walletRepository.save(Wallet.builder().user(user).diamondBalance(0L).build());

        pkg = diamondPackageRepository.save(DiamondPackage.builder()
                .name("Test Pack").diamonds(50).price(new BigDecimal("200.00")).currency("INR").active(true).build());
    }

    @Test
    void verifyingTheSamePaymentTwice_onlyCreditsDiamondsOnce() {
        PaymentOrderResponse order = paymentService.createOrder(user.getId(), new CreateOrderRequest(pkg.getId()));

        PaymentResponse first = paymentService.verify(user.getId(), new PaymentVerifyRequest(order.orderId(), null, null));
        assertEquals("SUCCESS", first.status());

        PaymentResponse second = paymentService.verify(user.getId(), new PaymentVerifyRequest(order.orderId(), null, null));
        assertEquals("SUCCESS", second.status());

        assertEquals(50L, walletRepository.findByUserId(user.getId()).orElseThrow().getDiamondBalance(),
                "diamonds must be credited exactly once even though verify() was called twice");
    }

    @Test
    void failedVerification_doesNotCreditDiamonds() {
        PaymentOrderResponse order = paymentService.createOrder(user.getId(), new CreateOrderRequest(pkg.getId()));

        assertThrows(RuntimeException.class, () ->
                paymentService.verify(user.getId(), new PaymentVerifyRequest(order.orderId(), "FAIL_TEST", null)));

        assertEquals(0L, walletRepository.findByUserId(user.getId()).orElseThrow().getDiamondBalance());
        assertEquals("FAILED", paymentRepository.findByOrderId(order.orderId()).orElseThrow().getStatus().name());
    }

    @Test
    void duplicateWebhookDelivery_onlyCreditsDiamondsOnce() throws Exception {
        PaymentOrderResponse order = paymentService.createOrder(user.getId(), new CreateOrderRequest(pkg.getId()));

        String payload = objectMapper.writeValueAsString(Map.of(
                "orderId", order.orderId(), "providerPaymentId", "mock_pay_webhook_1", "status", "SUCCESS"));
        String signature = mockPaymentProvider.sign(payload);

        paymentService.handleWebhook(payload, signature);
        paymentService.handleWebhook(payload, signature); // simulate the gateway retrying delivery

        assertEquals(50L, walletRepository.findByUserId(user.getId()).orElseThrow().getDiamondBalance());
    }

    @Test
    void webhookWithBadSignature_isRejectedAndCreditsNothing() {
        PaymentOrderResponse order = paymentService.createOrder(user.getId(), new CreateOrderRequest(pkg.getId()));
        String payload = "{\"orderId\":\"" + order.orderId() + "\",\"providerPaymentId\":\"x\",\"status\":\"SUCCESS\"}";

        assertThrows(RuntimeException.class, () -> paymentService.handleWebhook(payload, "not-a-valid-signature"));
        assertEquals(0L, walletRepository.findByUserId(user.getId()).orElseThrow().getDiamondBalance());
    }
}
