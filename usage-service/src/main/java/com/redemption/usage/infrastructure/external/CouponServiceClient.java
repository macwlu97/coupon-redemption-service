package com.redemption.usage.infrastructure.external;

import com.redemption.usage.infrastructure.external.dto.CouponInternalResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "coupon-service", url = "${app.coupon-service.url}")
public interface CouponServiceClient {

    /**
     * Calls coupon-service internal API to verify and lock the usage.
     */
    @PostMapping("/api/v1/internal/coupons/{code}/validate-and-increment")
    CouponInternalResponse validateAndIncrement(
            @PathVariable("code") String code,
            @RequestParam("countryCode") String countryCode);
}