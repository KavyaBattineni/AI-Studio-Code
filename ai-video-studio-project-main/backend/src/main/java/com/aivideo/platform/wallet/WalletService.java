package com.aivideo.platform.wallet;

import com.aivideo.platform.common.exception.InsufficientDiamondsException;
import com.aivideo.platform.common.exception.ResourceNotFoundException;
import com.aivideo.platform.user.User;
import com.aivideo.platform.user.UserRepository;
import com.aivideo.platform.wallet.dto.TransactionResponse;
import com.aivideo.platform.wallet.dto.WalletResponse;
import com.aivideo.platform.wallet.entity.TransactionType;
import com.aivideo.platform.wallet.entity.Wallet;
import com.aivideo.platform.wallet.entity.WalletTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * All diamond balance changes in the entire application go through debit()/credit() here,
 * and both take a PESSIMISTIC_WRITE lock on the wallet row for their whole transaction
 * (see WalletRepository.findByUserIdForUpdate). That is what makes "user has 5 diamonds,
 * two concurrent 5-diamond requests arrive" safe: the second transaction cannot even read
 * the wallet row until the first one has committed (deducted) or rolled back, so it always
 * sees the up-to-date balance before deciding whether it can proceed. The balance is also
 * protected at the database level by a CHECK (diamond_balance >= 0) constraint as defense
 * in depth.
 */
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final UserRepository userRepository;

    @Transactional
    public Wallet getOrCreateWallet(Long userId) {
        return walletRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            Wallet wallet = Wallet.builder().user(user).diamondBalance(0L).build();
            return walletRepository.save(wallet);
        });
    }

    @Transactional(readOnly = true)
    public WalletResponse getWallet(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        return new WalletResponse(wallet.getDiamondBalance());
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(Long userId, Pageable pageable) {
        return walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(TransactionResponse::from);
    }

    /**
     * Debits `amount` diamonds from the user's wallet. Requires an existing, sufficiently
     * funded wallet - throws InsufficientDiamondsException otherwise (never lets the balance
     * go negative). Runs in its own transaction (REQUIRES_NEW) by default is NOT used here on
     * purpose: callers that need the debit to be atomic with creating a related row (e.g. a
     * VideoJob) should call this from within their own @Transactional method so both succeed
     * or both roll back together.
     */
    @Transactional
    public WalletTransaction debit(Long userId, long amount, TransactionType type, String referenceId, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        if (wallet.getDiamondBalance() < amount) {
            throw new InsufficientDiamondsException("Insufficient diamond balance");
        }

        long before = wallet.getDiamondBalance();
        wallet.setDiamondBalance(before - amount);
        long after = wallet.getDiamondBalance();
        walletRepository.save(wallet);

        return recordTransaction(wallet, type, -amount, before, after, referenceId, description);
    }

    /** Credits `amount` diamonds to the user's wallet (purchases, refunds, rewards, admin adjustments). */
    @Transactional
    public WalletTransaction credit(Long userId, long amount, TransactionType type, String referenceId, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        long before = wallet.getDiamondBalance();
        wallet.setDiamondBalance(before + amount);
        long after = wallet.getDiamondBalance();
        walletRepository.save(wallet);

        return recordTransaction(wallet, type, amount, before, after, referenceId, description);
    }

    /** Used only by admins for manual balance corrections; amount may be positive or negative. */
    @Transactional
    public WalletTransaction adminAdjust(Long userId, long signedAmount, String description) {
        if (signedAmount == 0) {
            throw new IllegalArgumentException("Adjustment amount cannot be zero");
        }
        if (signedAmount > 0) {
            return credit(userId, signedAmount, TransactionType.ADMIN_ADJUSTMENT, "admin-adjustment", description);
        }
        return debit(userId, -signedAmount, TransactionType.ADMIN_ADJUSTMENT, "admin-adjustment", description);
    }

    @Transactional(readOnly = true)
    public long sumAmountByType(TransactionType type) {
        return walletTransactionRepository.sumAmountByType(type);
    }

    @Transactional(readOnly = true)
    public Page<com.aivideo.platform.wallet.dto.AdminTransactionResponse> adminSearchTransactions(
            TransactionType type, Long userId, java.time.Instant from, java.time.Instant to, Pageable pageable) {
        return walletTransactionRepository.adminSearch(type, userId, from, to, pageable)
                .map(com.aivideo.platform.wallet.dto.AdminTransactionResponse::from);
    }

    /** True if a transaction of this type already exists for this reference (idempotency guard). */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public boolean hasTransaction(String referenceId, TransactionType type) {
        return walletTransactionRepository.findFirstByReferenceIdAndTypeOrderByCreatedAtDesc(referenceId, type).isPresent();
    }

    private WalletTransaction recordTransaction(Wallet wallet, TransactionType type, long signedAmount,
                                                  long before, long after, String referenceId, String description) {
        WalletTransaction transaction = WalletTransaction.builder()
                .user(wallet.getUser())
                .wallet(wallet)
                .type(type)
                .amount(signedAmount)
                .balanceBefore(before)
                .balanceAfter(after)
                .referenceId(referenceId)
                .description(description)
                .build();
        return walletTransactionRepository.save(transaction);
    }
}
