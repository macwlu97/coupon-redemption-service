package com.redemption.usage.application;

import com.redemption.usage.domain.exception.UsageException;
import com.redemption.usage.domain.model.UsageHistory;
import com.redemption.usage.domain.repository.UsageHistoryRepository;
import com.redemption.usage.infrastructure.external.CouponServiceClient;
import com.redemption.usage.infrastructure.external.GeoIpService;
import com.redemption.usage.infrastructure.external.dto.CouponInternalResponse;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsageApplicationService {

    private final UsageHistoryRepository usageRepository;
    private final CouponServiceClient couponClient;
    private final GeoIpService geoIpService;

    /**
     * Entry point using Virtual Threads (implicit in Java 21+ Spring Boot 3.4)
     * We REMOVE @Transactional from here to avoid holding DB connections during network I/O.
     */
    @Retry(name = "couponServiceRetry")
    public String redeem(String couponCode, String userIp) {
        String code = couponCode.toUpperCase();
        log.info("Starting redemption flow for code: {} from IP: {}", couponCode, userIp);

        log.info("Starting redemption flow for code: {} from IP: {}", code, userIp);

        // 1. Idempotency check (Domain Layer)
        // Ensure this specific user/IP hasn't used this coupon already.
        if (usageRepository.existsByCouponCodeAndUserId(code, userIp)) {
            log.warn("Double redemption blocked: {} by {}", code, userIp);
            throw new UsageException.AlreadyRedeemed(code, userIp);
        }

        // 2. Resolve Location (Infrastructure Layer),
        // GeoIP - Always outside transaction
        // Moved here to decouple coupon-service from GeoIP concerns.
        String country = geoIpService.getCountryCode(userIp);

        // 3. Remote Validation & Update (Infrastructure - Feign Call)
        // We call coupon-service to perform global usage checks.
        CouponInternalResponse response = couponClient.validateAndIncrement(
                couponCode.toUpperCase(),
                country
        );

        // 4. Handle Specific Concurrency Error for Retry
        if (!response.success()) {
            if ("CONCURRENCY_ERROR".equals(response.errorCode())) {
                log.warn("Conflict in coupon-service for {}. Retry will be triggered.", couponCode);
                throw new UsageException.ConcurrencyConflict(couponCode);
            }
            throw new UsageException.RemoteServiceError(response.message(), response.errorCode());
        }

        // 5. Finalize - Only this part is Transactional
        usageRepository.save(new UsageHistory(code, userIp));
        log.info("Redemption successful for IP: {}", userIp);

        return country;
    }
}