package com.aivideo.platform.ads;

import com.aivideo.platform.ads.dto.AdVerifyRequest;
import com.aivideo.platform.ads.dto.RewardResponse;
import com.aivideo.platform.ads.dto.RewardStatusResponse;
import com.aivideo.platform.common.ApiResponse;
import com.aivideo.platform.security.SecurityUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rewards")
@RequiredArgsConstructor
@Tag(name = "Rewards")
public class RewardController {

    private final RewardService rewardService;

    @GetMapping("/status")
    public ApiResponse<RewardStatusResponse> status(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.success(rewardService.status(user.getId()));
    }

    @PostMapping("/ad/verify")
    public ApiResponse<RewardResponse> verifyAd(@AuthenticationPrincipal SecurityUser user,
                                                  @Valid @RequestBody AdVerifyRequest request) {
        return ApiResponse.success("Reward credited", rewardService.claimAdReward(user.getId(), request));
    }
}
