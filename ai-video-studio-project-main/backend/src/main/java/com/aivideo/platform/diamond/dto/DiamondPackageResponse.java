package com.aivideo.platform.diamond.dto;

import com.aivideo.platform.diamond.entity.DiamondPackage;

import java.math.BigDecimal;

public record DiamondPackageResponse(
        Long id,
        String name,
        int diamonds,
        BigDecimal price,
        String currency
) {
    public static DiamondPackageResponse from(DiamondPackage pkg) {
        return new DiamondPackageResponse(pkg.getId(), pkg.getName(), pkg.getDiamonds(), pkg.getPrice(), pkg.getCurrency());
    }
}
