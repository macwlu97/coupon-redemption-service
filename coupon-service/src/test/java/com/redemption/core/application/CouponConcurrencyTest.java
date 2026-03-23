package com.redemption.core.application;

import com.redemption.core.domain.model.Coupon;
import com.redemption.core.domain.repository.CouponRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.UnexpectedRollbackException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class CouponConcurrencyTest {

    @Autowired
    private CouponApplicationService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Test
    void shouldHandleConcurrentRedemptionsCorrectly() throws InterruptedException {
        // Given
        final String code = "CONCURRENCY-2026";
        final int numberOfThreads = 10;

        // Prepare clean state: usage limit = 1 ensures only one thread should succeed
        couponRepository.saveAndFlush(new Coupon(code, 1, "PL"));

        final var successes = new AtomicInteger();
        final var failures = new AtomicInteger();
        final var startLatch = new CountDownLatch(1);
        final var doneLatch = new CountDownLatch(numberOfThreads);

        // When - Using Java 21 Virtual Threads
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < numberOfThreads; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await(); // Synchronization point for simultaneous start

                        var response = couponService.processInternalRedemption(code, "PL");

                        if (response.success()) {
                            successes.incrementAndGet();
                        } else {
                            // Business failures (e.g. Limit Exceeded) or caught Locking exceptions
                            failures.incrementAndGet();
                        }
                    } catch (UnexpectedRollbackException e) {
                        // Spring throws this when the transaction fails at the COMMIT stage (Proxy level)
                        log.warn("Transaction rolled back due to concurrent modification");
                        failures.incrementAndGet();
                    } catch (Exception e) {
                        log.error("Unexpected thread error", e);
                        failures.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown(); // Start all virtual threads
            doneLatch.await();      // Wait for completion
        }

        // Then
        var finalCoupon = couponRepository.findByCode(code).orElseThrow();

        log.info("Final results - Successes: {}, Failures: {}, Final usage: {}",
                successes.get(), failures.get(), finalCoupon.getCurrentUsage());

        // Assertions
        assertThat(finalCoupon.getCurrentUsage()).isEqualTo(1);
        assertThat(successes.get()).isEqualTo(1);
        assertThat(failures.get()).isEqualTo(9);
    }
}
