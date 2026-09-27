package com.aivideo.platform.wallet;

import com.aivideo.platform.wallet.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    Page<WalletTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<WalletTransaction> findFirstByReferenceIdAndTypeOrderByCreatedAtDesc(
            String referenceId, com.aivideo.platform.wallet.entity.TransactionType type);

    @Query("""
        select t from WalletTransaction t
        where (:type is null or t.type = :type)
          and (:userId is null or t.user.id = :userId)
          and (:from is null or t.createdAt >= :from)
          and (:to is null or t.createdAt <= :to)
        order by t.createdAt desc
        """)
    Page<WalletTransaction> adminSearch(@Param("type") com.aivideo.platform.wallet.entity.TransactionType type,
                                         @Param("userId") Long userId,
                                         @Param("from") Instant from,
                                         @Param("to") Instant to,
                                         Pageable pageable);

    @Query("select coalesce(sum(t.amount), 0) from WalletTransaction t where t.type = :type")
    long sumAmountByType(@Param("type") com.aivideo.platform.wallet.entity.TransactionType type);
}
