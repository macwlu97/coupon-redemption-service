package com.redemption.usage.infrastructure.external;

import com.redemption.usage.infrastructure.external.dto.CouponInternalResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "coupon-service", url = "${app.coupon-service.url}")
public interface CouponServiceClient {

    /**
     * Calls coupon-service internal API with Circuit Breaker protection.
     * Name "internalServiceCB" must match the configuration in application.yml.
     */
    @CircuitBreaker(name = "internalServiceCB", fallbackMethod = "fallbackValidateAndIncrement")
    @PostMapping("/api/v1/internal/coupons/{code}/validate-and-increment")
    CouponInternalResponse validateAndIncrement(
            @PathVariable("code") String code,
            @RequestParam("countryCode") String countryCode);

    /**
     * Fallback method executed when the circuit is OPEN or an error occurs.
     * Must have the same signature as the original method plus a Throwable parameter.
     */
    default CouponInternalResponse fallbackValidateAndIncrement(String code, String countryCode, Throwable t) {
        // log.error("Circuit Breaker triggered for coupon-service. Reason: {}", t.getMessage());

        return new CouponInternalResponse(
                false,
                "SYSTEM_TEMPORARILY_UNAVAILABLE",
                "We are experiencing technical difficulties with coupon validation. Please try again in a few minutes."
        );
    }
}