package com.redemption.usage.application;

import com.redemption.usage.domain.exception.UsageException;
import com.redemption.usage.domain.model.UsageHistory;
import com.redemption.usage.domain.repository.UsageHistoryRepository;
import com.redemption.usage.infrastructure.external.CouponServiceClient;
import com.redemption.usage.infrastructure.external.GeoIpClient;
import com.redemption.usage.infrastructure.external.dto.CouponInternalResponse;
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
    private final GeoIpClient geoIpClient;

    /**
     * Main entry point for redeeming a coupon in a microservice architecture.
     */
    @Transactional
    public void redeem(String couponCode, String userIp) {
        log.info("Starting redemption flow for code: {} from IP: {}", couponCode, userIp);

        // 1. Resolve Location (Infrastructure Layer)
        // Moved here to decouple coupon-service from GeoIP concerns.
        String country = geoIpClient.fetchCountryCode(userIp);

        // 2. Idempotency check (Domain Layer)
        // Ensure this specific user/IP hasn't used this coupon already.
        if (usageRepository.existsByCouponCodeAndUserId(couponCode.toUpperCase(), userIp)) {
            log.warn("User with IP {} already redeemed coupon {}", userIp, couponCode);
            throw new UsageException.AlreadyRedeemed(couponCode.toUpperCase(), userIp);
        }

        // 3. Remote Validation & Update (Infrastructure - Feign Call)
        // We call coupon-service to perform global usage checks.
        CouponInternalResponse response = couponClient.validateAndIncrement(
                couponCode.toUpperCase(),
                country
        );

        if (!response.success()) {
            log.error("Coupon service rejected redemption: {} - {}",
                    response.errorCode(),
                    response.message());
            throw new UsageException.RemoteServiceError(response.message(), response.errorCode());
        }

        // 4. Record Audit Log (Domain Layer)
        // Local history storage for reporting and future limit checks.
        var history = new UsageHistory(couponCode.toUpperCase(), userIp);
        usageRepository.save(history);

        log.info("Redemption successfully completed for IP: {}", userIp);
    }
}