package com.aivideo.platform.admin;

import com.aivideo.platform.admin.dto.DashboardStatsResponse;
import com.aivideo.platform.admin.dto.RevenueResponse;
import com.aivideo.platform.common.ApiResponse;
import com.aivideo.platform.moderation.ModerationService;
import com.aivideo.platform.moderation.dto.AdminModerationResponse;
import com.aivideo.platform.moderation.entity.ModerationDecision;
import com.aivideo.platform.payment.PaymentService;
import com.aivideo.platform.payment.dto.AdminPaymentResponse;
import com.aivideo.platform.payment.entity.PaymentStatus;
import com.aivideo.platform.user.UserService;
import com.aivideo.platform.user.UserStatus;
import com.aivideo.platform.user.dto.AdminUserResponse;
import com.aivideo.platform.user.dto.UpdateUserStatusRequest;
import com.aivideo.platform.video.VideoService;
import com.aivideo.platform.video.dto.AdminVideoJobResponse;
import com.aivideo.platform.video.entity.VideoStatus;
import com.aivideo.platform.wallet.WalletService;
import com.aivideo.platform.wallet.dto.AdminTransactionResponse;
import com.aivideo.platform.wallet.entity.TransactionType;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * Every endpoint here requires ROLE_ADMIN (enforced by SecurityConfig's
 * .requestMatchers("/api/admin/**").hasRole("ADMIN") - never by anything client-side).
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin")
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;
    private final PaymentService paymentService;
    private final VideoService videoService;
    private final ModerationService moderationService;
    private final WalletService walletService;

    @GetMapping("/dashboard")
    public ApiResponse<DashboardStatsResponse> dashboard() {
        return ApiResponse.success(adminService.dashboard());
    }

    @GetMapping("/users")
    public ApiResponse<Page<AdminUserResponse>> users(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(userService.adminSearch(search, status, PageRequest.of(page, size)));
    }

    @PutMapping("/users/{id}/status")
    public ApiResponse<AdminUserResponse> updateUserStatus(@PathVariable Long id,
                                                             @Valid @RequestBody UpdateUserStatusRequest request) {
        return ApiResponse.success("User status updated", userService.updateStatus(id, request.status()));
    }

    @GetMapping("/payments")
    public ApiResponse<Page<AdminPaymentResponse>> payments(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(paymentService.adminSearch(status, PageRequest.of(page, size)));
    }

    @GetMapping("/videos")
    public ApiResponse<Page<AdminVideoJobResponse>> videos(
            @RequestParam(required = false) VideoStatus status,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(videoService.adminSearch(status, userId, PageRequest.of(page, size)));
    }

    @GetMapping("/moderation")
    public ApiResponse<Page<AdminModerationResponse>> moderation(
            @RequestParam(required = false) ModerationDecision decision,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(moderationService.adminSearch(decision, PageRequest.of(page, size)));
    }

    @GetMapping("/transactions")
    public ApiResponse<Page<AdminTransactionResponse>> transactions(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(walletService.adminSearchTransactions(type, userId, from, to, PageRequest.of(page, size)));
    }

    @GetMapping("/revenue")
    public ApiResponse<RevenueResponse> revenue() {
        return ApiResponse.success(adminService.revenue());
    }
}
