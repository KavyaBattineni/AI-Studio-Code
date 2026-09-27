package com.aivideo.platform.wallet;

import com.aivideo.platform.wallet.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUserId(Long userId);

    /**
     * Locks the wallet row (SELECT ... FOR UPDATE) for the duration of the caller's
     * transaction. This is THE mechanism that prevents the double-spend race described in
     * the platform's wallet-concurrency requirement: if two requests try to debit the same
     * wallet at the same time, the second one blocks here until the first transaction commits
     * or rolls back, then re-reads the now-current balance before deciding whether it can
     * proceed. Combined with the balance >= 0 check in WalletService, this guarantees at most
     * one of two simultaneous "spend the same diamonds" requests can succeed.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.user.id = :userId")
    Optional<Wallet> findByUserIdForUpdate(@Param("userId") Long userId);
}
