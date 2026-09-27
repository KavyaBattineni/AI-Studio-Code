package com.aivideo.platform.ads;

import com.aivideo.platform.ads.entity.AdReward;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdRewardRepository extends JpaRepository<AdReward, Long> {

    Optional<AdReward> findByProviderAndVerificationId(String provider, String verificationId);
}
