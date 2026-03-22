package com.redemption.usage.infrastructure.external.dto;

/**
 * Internal DTO defined as a Java 21 Record.
 * Static factory methods are perfectly valid here.
 */
public record CouponInternalResponse(
        boolean success,
        String errorCode,
        String message
) {
    public static CouponInternalResponse ok() {
        return new CouponInternalResponse(true, null, "OK");
    }

    public static CouponInternalResponse failure(String code, String msg) {
        return new CouponInternalResponse(false, code, msg);
    }
}