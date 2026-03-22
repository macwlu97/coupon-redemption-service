package com.redemption.core.api.internal;

import com.redemption.core.application.CouponApplicationService;
import com.redemption.core.api.internal.dto.CouponInternalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal/coupons")
@RequiredArgsConstructor
public class InternalCouponController {

    private final CouponApplicationService service;

    /**
     * Dedicated endpoint for usage-service inter-service communication.
     */
    @PostMapping("/{code}/validate-and-increment")
    public CouponInternalResponse validateAndIncrement(
            @PathVariable String code,
            @RequestParam String countryCode) {
        return service.processInternalRedemption(code, countryCode);
    }
}
