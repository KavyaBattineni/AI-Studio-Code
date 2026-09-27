package com.aivideo.platform.diamond;

import com.aivideo.platform.common.ApiResponse;
import com.aivideo.platform.diamond.dto.DiamondPackageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/diamonds")
@RequiredArgsConstructor
@Tag(name = "Diamonds")
public class DiamondController {

    private final DiamondService diamondService;

    @GetMapping("/packages")
    public ApiResponse<List<DiamondPackageResponse>> packages() {
        return ApiResponse.success(diamondService.listActivePackages());
    }
}
