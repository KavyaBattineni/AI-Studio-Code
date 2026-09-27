package com.aivideo.platform.payment;

import com.aivideo.platform.common.ApiResponse;
import com.aivideo.platform.payment.dto.CreateOrderRequest;
import com.aivideo.platform.payment.dto.PaymentOrderResponse;
import com.aivideo.platform.payment.dto.PaymentResponse;
import com.aivideo.platform.payment.dto.PaymentVerifyRequest;
import com.aivideo.platform.security.SecurityUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-order")
    public ApiResponse<PaymentOrderResponse> createOrder(@AuthenticationPrincipal SecurityUser user,
                                                            @Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.success(paymentService.createOrder(user.getId(), request));
    }

    @PostMapping("/verify")
    public ApiResponse<PaymentResponse> verify(@AuthenticationPrincipal SecurityUser user,
                                                 @Valid @RequestBody PaymentVerifyRequest request) {
        return ApiResponse.success("Payment verified. Diamonds credited.", paymentService.verify(user.getId(), request));
    }

    @GetMapping("/history")
    public ApiResponse<Page<PaymentResponse>> history(@AuthenticationPrincipal SecurityUser user,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(paymentService.history(user.getId(), PageRequest.of(page, size)));
    }
}
