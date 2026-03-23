package com.redemption.core.application;

import com.redemption.core.api.internal.dto.CouponInternalResponse;
import com.redemption.core.api.rest.dto.CreateCouponRequest;
import com.redemption.core.api.rest.dto.CouponResponse;
import com.redemption.core.domain.exception.CouponException;
import com.redemption.core.domain.model.Coupon;
import com.redemption.core.domain.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponApplicationService {

    private final CouponRepository repository;

    /**
     * Handles internal redemption request from usage-service.
     * Validates business rules and increments usage in a single transaction.
     * Uses Java 21 features and handles concurrency via Optimistic Locking.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CouponInternalResponse processInternalRedemption(String code, String countryCode) {
        try {
            // Finding coupon using Java 21 Optional
            var coupon = repository.findByCode(code.toUpperCase())
                    .orElseThrow(() -> new CouponException.NotFound(code));

            // Domain logic execution (DDD approach)
            coupon.redeem(countryCode);

            // Persistence - @Version in Coupon entity handles optimistic locking
            repository.save(coupon);

            log.info("Redemption successful for coupon: {}", code);
            return CouponInternalResponse.ok();

        } catch (Exception e) {
            return handleException(e, code);
        }

    }

    private CouponInternalResponse handleException(Exception e, String code) {
        // Pattern Matching (Java 17/21)
        return switch (e) {
            case CouponException.NotFound ex -> {
                log.warn("Coupon not found: {}", code);
                yield CouponInternalResponse.failure("COUPON_NOT_FOUND", ex.getMessage());
            }
            case CouponException.InvalidCountry ex -> {
                log.warn("Country mismatch for coupon: {}", code);
                yield CouponInternalResponse.failure("INVALID_COUNTRY", ex.getMessage());
            }
            case CouponException.LimitExceeded ex -> {
                log.warn("Limit exceeded for: {}", code);
                yield CouponInternalResponse.failure("LIMIT_EXCEEDED", ex.getMessage());
            }
            case ObjectOptimisticLockingFailureException ex ->
                    CouponInternalResponse.failure("CONCURRENCY_ERROR", "Retry needed");
            default -> {
                log.error("Unexpected error", e);
                yield CouponInternalResponse.failure("SYSTEM_ERROR", "Internal error");
            }
        };
    }

    @Transactional
    public CouponResponse createCoupon(CreateCouponRequest request) {
        repository.findByCode(request.code().toUpperCase())
                .ifPresent(c -> { throw new CouponException.AlreadyExists(request.code()); });

        var newCoupon = new Coupon(
                request.code(),
                request.usageLimit(),
                request.targetCountry()
        );

        return mapToResponse(repository.save(newCoupon));
    }

    @Transactional(readOnly = true)
    public Page<CouponResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable)
                .map(this::mapToResponse);
    }

    private CouponResponse mapToResponse(Coupon coupon) {
        return new CouponResponse(
                coupon.getCode(),
                coupon.getUsageLimit(),
                coupon.getCurrentUsage(),
                coupon.getTargetCountry(),
                coupon.getCreatedAt()
        );
    }
}