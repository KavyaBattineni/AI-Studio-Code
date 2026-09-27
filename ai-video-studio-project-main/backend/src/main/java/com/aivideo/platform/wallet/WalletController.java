package com.aivideo.platform.wallet;

import com.aivideo.platform.common.ApiResponse;
import com.aivideo.platform.security.SecurityUser;
import com.aivideo.platform.wallet.dto.TransactionResponse;
import com.aivideo.platform.wallet.dto.WalletResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Tag(name = "Wallet")
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public ApiResponse<WalletResponse> getWallet(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.success(walletService.getWallet(user.getId()));
    }

    @GetMapping("/transactions")
    public ApiResponse<Page<TransactionResponse>> getTransactions(
            @AuthenticationPrincipal SecurityUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(walletService.getTransactions(user.getId(), PageRequest.of(page, size)));
    }
}
