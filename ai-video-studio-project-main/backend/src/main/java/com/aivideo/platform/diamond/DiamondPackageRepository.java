package com.aivideo.platform.diamond;

import com.aivideo.platform.diamond.entity.DiamondPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiamondPackageRepository extends JpaRepository<DiamondPackage, Long> {

    List<DiamondPackage> findByActiveTrueOrderByDiamondsAsc();
}
