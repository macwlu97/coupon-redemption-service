//package com.redemption.core.application;
//
//import com.redemption.core.domain.model.Coupon;
//import com.redemption.core.domain.repository.CouponRepository;
//import com.redemption.core.infrastructure.external.GeoIpClient;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.BDDMockito.given;
//
//@SpringBootTest
//class CouponConcurrencyTest {
//
//    @Autowired
//    private CouponApplicationService couponService;
//
//    @Autowired
//    private CouponRepository couponRepository;
//
//    @MockitoBean
//    private GeoIpClient geoIpClient;
//
//    @Test
//    @DisplayName("Concurrency: Only one user should be able to redeem a coupon with limit 1")
//    void shouldHandleRaceConditionWithOptimisticLocking() throws InterruptedException {
//        // Given: A coupon with a strict limit of 1
//        String code = "FLASH-SALE";
//        couponRepository.save(new Coupon(code, 1, "PL"));
//        given(geoIpClient.fetchCountryCode(anyString())).willReturn("PL");
//
//        int numberOfThreads = 10;
//        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
//        CountDownLatch latch = new CountDownLatch(1); // Synchronization start
//        CountDownLatch finishLatch = new CountDownLatch(numberOfThreads); // Synchronization end
//
//        AtomicInteger successCount = new AtomicInteger();
//        AtomicInteger failureCount = new AtomicInteger();
//
//        // When: 10 threads try to redeem at the same time
//        for (int i = 0; i < numberOfThreads; i++) {
//            executor.execute(() -> {
//                try {
//                    latch.await(); // Wait for the "Go!" signal
//                    couponService.processRedemption(code, "1.2.3.4");
//                    successCount.incrementAndGet();
//                } catch (Exception e) {
//                    failureCount.incrementAndGet();
//                } finally {
//                    finishLatch.countDown();
//                }
//            });
//        }
//
//        latch.countDown(); // "Go!" - Start all threads
//        finishLatch.await(); // Wait for all threads to finish
//
//        // Then: Usage must be exactly 1, no matter how many threads tried
//        Coupon finalCoupon = couponRepository.findByCode(code).orElseThrow();
//
//        assertThat(finalCoupon.getCurrentUsage())
//                .as("Usage should not exceed the limit even under heavy load")
//                .isEqualTo(1);
//
//        assertThat(successCount.get())
//                .as("Only one thread should have succeeded")
//                .isEqualTo(1);
//
//        assertThat(failureCount.get())
//                .as("Remaining threads should have failed due to locking or limit")
//                .isEqualTo(9);
//    }
//}