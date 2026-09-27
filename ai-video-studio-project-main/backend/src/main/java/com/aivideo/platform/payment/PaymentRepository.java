package com.aivideo.platform.payment;

import com.aivideo.platform.payment.entity.Payment;
import com.aivideo.platform.payment.entity.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(String orderId);

    /** Locked read used by verify/webhook handling so two near-simultaneous confirmations
     *  for the same order can never both credit diamonds (see PaymentService). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.orderId = :orderId")
    Optional<Payment> findByOrderIdForUpdate(@Param("orderId") String orderId);

    Page<Payment> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("""
        select p from Payment p
        where (:status is null or p.status = :status)
        order by p.createdAt desc
        """)
    Page<Payment> adminSearch(@Param("status") PaymentStatus status, Pageable pageable);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.status = 'SUCCESS'")
    java.math.BigDecimal sumSuccessfulAmount();

    long countByStatus(PaymentStatus status);
}
