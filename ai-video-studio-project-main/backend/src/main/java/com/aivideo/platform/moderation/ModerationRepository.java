package com.aivideo.platform.moderation;

import com.aivideo.platform.moderation.entity.ModerationResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ModerationRepository extends JpaRepository<ModerationResult, Long> {

    long countByDecision(com.aivideo.platform.moderation.entity.ModerationDecision decision);

    @Query("""
        select m from ModerationResult m
        where (:decision is null or m.decision = :decision)
        order by m.createdAt desc
        """)
    Page<ModerationResult> adminSearch(@Param("decision") com.aivideo.platform.moderation.entity.ModerationDecision decision,
                                        Pageable pageable);
}
