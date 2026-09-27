package com.aivideo.platform.wallet;

import com.aivideo.platform.common.exception.InsufficientDiamondsException;
import com.aivideo.platform.user.User;
import com.aivideo.platform.user.UserRepository;
import com.aivideo.platform.user.UserRole;
import com.aivideo.platform.user.UserStatus;
import com.aivideo.platform.wallet.entity.TransactionType;
import com.aivideo.platform.wallet.entity.Wallet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class WalletConcurrencyTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * The exact scenario called out in the platform spec: a wallet holding 5 diamonds
     * receives two simultaneous requests to spend 5 diamonds each. Because WalletService.debit
     * takes a PESSIMISTIC_WRITE lock on the wallet row for the whole transaction, the two
     * threads cannot both read balance=5 and both succeed - the second one is forced to wait
     * for the first transaction to commit, then correctly sees balance=0 and is rejected.
     */
    @Test
    void onlyOneOfTwoSimultaneousDebitsSucceeds_whenBalanceCoversExactlyOne() throws Exception {
        User user = userRepository.save(User.builder()
                .name("Concurrency Test User")
                .email("concurrency-" + System.nanoTime() + "@example.com")
                .mobile("9000000000")
                .passwordHash("irrelevant-for-this-test")
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .build());

        Wallet wallet = walletRepository.save(Wallet.builder().user(user).diamondBalance(5L).build());

        int threadCount = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger(0);
        AtomicInteger rejected = new AtomicInteger(0);

        Callable<Void> spendFiveDiamonds = () -> {
            readyLatch.countDown();
            startLatch.await(10, TimeUnit.SECONDS); // line both threads up to fire together
            try {
                walletService.debit(user.getId(), 5L, TransactionType.VIDEO_GENERATION, "concurrency-test", "spend 5");
                succeeded.incrementAndGet();
            } catch (InsufficientDiamondsException expected) {
                rejected.incrementAndGet();
            }
            return null;
        };

        Future<Void> f1 = pool.submit(spendFiveDiamonds);
        Future<Void> f2 = pool.submit(spendFiveDiamonds);

        assertTrue(readyLatch.await(10, TimeUnit.SECONDS), "both threads should reach the start line");
        startLatch.countDown(); // release both at (as close to) the same instant as possible

        f1.get(15, TimeUnit.SECONDS);
        f2.get(15, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(1, succeeded.get(), "exactly one of the two concurrent debits should succeed");
        assertEquals(1, rejected.get(), "exactly one of the two concurrent debits should be rejected");

        Wallet reloaded = walletRepository.findById(wallet.getId()).orElseThrow();
        assertEquals(0L, reloaded.getDiamondBalance(), "balance must be exactly 0, never negative");
    }

    @Test
    void debitBeyondBalance_throwsAndLeavesBalanceUnchanged() {
        User user = userRepository.save(User.builder()
                .name("Insufficient Funds User")
                .email("insufficient-" + System.nanoTime() + "@example.com")
                .mobile("9000000001")
                .passwordHash("irrelevant")
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .build());
        walletRepository.save(Wallet.builder().user(user).diamondBalance(3L).build());

        assertThrows(InsufficientDiamondsException.class, () ->
                walletService.debit(user.getId(), 5L, TransactionType.VIDEO_GENERATION, "ref", "over-spend"));

        assertEquals(3L, walletRepository.findByUserId(user.getId()).orElseThrow().getDiamondBalance());
    }
}
