package com.redemption.core.api.internal;

import com.redemption.core.application.CouponApplicationService;
import com.redemption.core.api.internal.dto.CouponInternalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal/coupons")
@RequiredArgsConstructor
@Tag(name = "Internal API", description = "Inter-service communication endpoints (strictly for usage-service)")
public class InternalCouponController {

    private final CouponApplicationService service;

    /**
     * Dedicated endpoint for usage-service inter-service communication.
     */
    @PostMapping("/{code}/validate-and-increment")
    @Operation(summary = "Validate and increment usage", description = "Internal endpoint to check coupon validity and increment its usage counter atomically.")
    public CouponInternalResponse validateAndIncrement(
            @PathVariable String code,
            @RequestParam String countryCode) {
        return service.processInternalRedemption(code, countryCode);
    }
}
