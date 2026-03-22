package com.redemption.core.domain.exception;

import lombok.Getter;

/**
 * Sealed class hierarchy for domain-specific business errors.
 * This ensures that all possible coupon errors are known at compile time.
 */
@Getter
public sealed abstract class CouponException extends RuntimeException {
    private final String code;

    protected CouponException(String message, String code) {
        super(message);
        this.code = code;
    }

    public static final class LimitExceeded extends CouponException {
        public LimitExceeded(String couponCode) {
            super("Usage limit reached for coupon: " + couponCode, "LIMIT_EXCEEDED");
        }
    }

    public static final class InvalidCountry extends CouponException {
        public InvalidCountry(String actual, String expected) {
            super("Invalid country: " + actual + ". Expected: " + expected, "INVALID_COUNTRY");
        }
    }

    public static final class NotFound extends CouponException {
        public NotFound(String couponCode) {
            super("Coupon not found: " + couponCode, "NOT_FOUND");
        }
    }

    public static final class AlreadyExists extends CouponException {
        public AlreadyExists(String couponCode) {
            super("Coupon with code " + couponCode + " already exists", "ALREADY_EXISTS");
        }
    }
}