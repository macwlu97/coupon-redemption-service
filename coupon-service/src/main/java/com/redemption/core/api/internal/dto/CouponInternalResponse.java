package com.redemption.core.api.internal.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Internal response for inter-service coupon validation")
public record CouponInternalResponse(
        @Schema(description = "Indicates if the validation and increment was successful", example = "true")
        boolean success,

        @Schema(description = "Business error code if success is false", example = "COUPON_NOT_FOUND", nullable = true)
        String errorCode,

        @Schema(description = "Human-readable message regarding the operation", example = "Redemption successful")
        String message
) {
    public static CouponInternalResponse ok() {
        return new CouponInternalResponse(true, null, "OK");
    }

    public static CouponInternalResponse failure(String code, String msg) {
        return new CouponInternalResponse(false, code, msg);
    }
}