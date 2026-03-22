package com.redemption.core.api.internal.dto;

public record CouponInternalResponse(boolean success, String errorCode, String message) {
    public static CouponInternalResponse ok() {
        return new CouponInternalResponse(true, null, "OK");
    }
    public static CouponInternalResponse failure(String code, String msg) {
        return new CouponInternalResponse(false, code, msg);
    }
}