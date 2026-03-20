package com.redemption.core.application;

import com.redemption.core.api.rest.dto.CreateCouponRequest;
import com.redemption.core.api.rest.dto.CouponResponse;
import com.redemption.core.domain.exception.CouponException;
import com.redemption.core.domain.model.Coupon;
import com.redemption.core.domain.repository.CouponRepository;
import com.redemption.core.infrastructure.external.GeoIpClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponApplicationService {

    private final CouponRepository repository;
    private final GeoIpClient geoIpClient;

    /**
     * Coupon redemption - business logic.
     */
    @Transactional
    public void processRedemption(String code, String ip) {
        // 1. Fetching the country from an external API.
        String countryCode = geoIpClient.fetchCountryCode(ip);

        // 2. Finding the coupon (using Java 21 Optional).
        Coupon coupon = repository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new CouponException.NotFound(code));

        // 3. Executing logic within the domain model (DDD).
        coupon.redeem(countryCode);

        // 4. Saving changes (Hibernate will handle @Version automatically).
        repository.save(coupon);
    }

    /**
     * Creates a new coupon and maps it to a DTO.
     */
    @Transactional
    public CouponResponse createCoupon(CreateCouponRequest request) {
        repository.findByCode(request.code().toUpperCase())
                .ifPresent(c -> { throw new IllegalStateException("Coupon already exists"); });

        Coupon newCoupon = new Coupon(
                request.code(),
                request.usageLimit(),
                request.targetCountry()
        );

        Coupon saved = repository.save(newCoupon);
        return mapToResponse(saved);
    }

    /**
     * Retrieves all coupons.
     */
    @Transactional(readOnly = true)
    public List<CouponResponse> findAll() {
        return repository.findAll().stream()
                .map(this::mapToResponse)
                .toList(); // Java 21 stream to list
    }

    /**
     * Private helper method for mapping (a mapper).
     */
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