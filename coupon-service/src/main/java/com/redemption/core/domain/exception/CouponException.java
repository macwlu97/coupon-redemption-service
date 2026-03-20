package com.redemption.core.domain.exception;

import lombok.Getter;

@Getter
public sealed abstract class CouponException extends RuntimeException {
    private final String code;

    protected CouponException(String message, String code) {
        super(message);
        this.code = code;
    }

    public static final class LimitExceeded extends CouponException {
        public LimitExceeded(String code) {
            super("Limit reached for coupon: " + code, code);
        }
    }

    public static final class InvalidCountry extends CouponException {
        public InvalidCountry(String actual, String expected) {
            super("Invalid country: " + actual + ". Expected: " + expected, "INVALID_COUNTRY");
        }
    }

    public static final class NotFound extends CouponException {
        public NotFound(String code) {
            super("Coupon not found: " + code, code);
        }
    }
}