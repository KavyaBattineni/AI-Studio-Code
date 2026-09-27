package com.aivideo.platform.video;

import com.aivideo.platform.video.entity.VideoJob;
import com.aivideo.platform.video.entity.VideoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VideoJobRepository extends JpaRepository<VideoJob, Long> {

    Page<VideoJob> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<VideoJob> findByIdAndUserId(Long id, Long userId);

    long countByStatus(VideoStatus status);

    @Query("""
        select v from VideoJob v
        where (:status is null or v.status = :status)
          and (:userId is null or v.user.id = :userId)
        order by v.createdAt desc
        """)
    Page<VideoJob> adminSearch(@Param("status") VideoStatus status, @Param("userId") Long userId, Pageable pageable);
}
