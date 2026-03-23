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

        } catch (ObjectOptimisticLockingFailureException e) {
            log.error("Concurrency conflict for coupon: {}", code);
            return CouponInternalResponse.failure("CONCURRENCY_ERROR", "Concurrent update detected");
        } catch (CouponException e) {
            log.warn("Business rule violation: {}", e.getMessage());
            return CouponInternalResponse.failure(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("System error during redemption", e);
            return CouponInternalResponse.failure("SYSTEM_ERROR", "Unexpected failure");
        }
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