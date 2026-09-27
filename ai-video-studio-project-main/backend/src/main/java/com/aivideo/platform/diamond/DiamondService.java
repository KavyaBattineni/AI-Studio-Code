package com.aivideo.platform.diamond;

import com.aivideo.platform.common.exception.ResourceNotFoundException;
import com.aivideo.platform.diamond.dto.DiamondPackageResponse;
import com.aivideo.platform.diamond.entity.DiamondPackage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiamondService {

    private final DiamondPackageRepository diamondPackageRepository;

    @Transactional(readOnly = true)
    public List<DiamondPackageResponse> listActivePackages() {
        return diamondPackageRepository.findByActiveTrueOrderByDiamondsAsc().stream()
                .map(DiamondPackageResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DiamondPackage getActivePackage(Long packageId) {
        DiamondPackage pkg = diamondPackageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Diamond package not found"));
        if (!pkg.isActive()) {
            throw new ResourceNotFoundException("This diamond package is no longer available");
        }
        return pkg;
    }
}
