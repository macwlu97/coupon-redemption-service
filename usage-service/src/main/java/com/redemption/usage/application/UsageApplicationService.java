package com.redemption.usage.application;

import com.redemption.usage.domain.exception.UsageException;
import com.redemption.usage.domain.model.UsageHistory;
import com.redemption.usage.domain.repository.UsageHistoryRepository;
import com.redemption.usage.infrastructure.external.CouponServiceClient;
import com.redemption.usage.infrastructure.external.GeoIpService;
import com.redemption.usage.infrastructure.external.dto.CouponInternalResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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
    private final UsageHistoryService historyService; // Delegating DB write
    private final CouponServiceClient couponClient;
    private final GeoIpService geoIpService;

    /**
     * Orchestrates the redemption flow.
     * Note: @Transactional is REMOVED from the main entry point to prevent
     * holding DB connections during blocking network I/O (GeoIP/Feign).
     */
    @Retry(name = "couponServiceRetry")
    @CircuitBreaker(name = "internalServiceCB", fallbackMethod = "handleRedeemFallback")
    public String redeem(final String couponCode, final String userIp) {
        final String code = couponCode.toUpperCase();

        final String cleanIp = extractClientIp(userIp);

        log.info("Starting redemption flow | Code: {} | IP: {}", code, cleanIp);

        // 1. Idempotency Check (Read-only, no heavy transaction needed)
        validateIdempotency(code, cleanIp);

        // 2. Resolve Location (External I/O - No DB connection held)
        String country = geoIpService.getCountryCode(cleanIp);

        // 3. Remote Validation (External I/O - No DB connection held)
        validateWithRemoteService(code, country);

        // 4. Finalize - Short-lived transaction for DB write only
        // this is a real transaction via Proxy
        historyService.persistUsage(code, cleanIp);

        log.info("Redemption successful | Code: {} | IP: {}", code, cleanIp);
        return country;
    }

    private void validateIdempotency(String code, String userId) {
        if (usageRepository.existsByCouponCodeAndUserId(code, userId)) {
            log.warn("Blocking double redemption | Code: {} | User: {}", code, userId);
            throw new UsageException.AlreadyRedeemed(code, userId);
        }
    }

    private void validateWithRemoteService(String code, String country) {
        CouponInternalResponse response = couponClient.validateAndIncrement(code, country);

        if (!response.success()) {
            handleRemoteError(code, response);
        }
    }

    private void handleRemoteError(String code, CouponInternalResponse response) {
        String error = response.errorCode();
        if ("CONCURRENCY_ERROR".equals(error)) {
            throw new UsageException.ConcurrencyConflict(code);
        } else if ("INVALID_COUNTRY".equals(error)) {
            throw new UsageException.InvalidCountry(code);
        }
        throw new UsageException.RemoteServiceError(response.message(), error);
    }

    /**
     * Fallback for Resilience4j. Handles circuit breaker opening or service failures.
     */
    public String handleRedeemFallback(String code, String ip, Throwable t) throws Throwable {
        if (t instanceof UsageException.AlreadyRedeemed
                || t instanceof UsageException.ConcurrencyConflict
                || t instanceof UsageException.InvalidCountry) {
            log.debug("Business exception in redemption: {}. Bypassing fallback.", t.getMessage());
            throw t;
        }

        log.error("Resilience4j Triggered | Reason: {} | Code: {}", t.getMessage(), code);
        throw new UsageException.RemoteServiceError("External service currently unavailable", "SERVICE_UNAVAILABLE");
    }

    private String extractClientIp(String userIp) {
        if (userIp == null || userIp.isEmpty()) {
            return "unknown";
        }
        // X-Forwarded-For: client, proxy1, proxy2...
        // Bierzemy tylko to, co jest przed pierwszym przecinkiem
        return userIp.split(",")[0].trim();
    }
}