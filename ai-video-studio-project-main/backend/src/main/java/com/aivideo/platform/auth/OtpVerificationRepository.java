package com.aivideo.platform.auth;

import com.aivideo.platform.auth.entity.OtpPurpose;
import com.aivideo.platform.auth.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(Long userId, OtpPurpose purpose);
}
