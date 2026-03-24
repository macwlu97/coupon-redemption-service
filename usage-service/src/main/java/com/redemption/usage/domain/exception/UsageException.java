package com.redemption.usage.domain.exception;

import lombok.Getter;

@Getter
public sealed abstract class UsageException extends RuntimeException {
    private final String errorCode;

    protected UsageException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public static final class ConcurrencyConflict extends UsageException {
        public ConcurrencyConflict(String couponCode) {
            super("Conflict detected for coupon: " + couponCode, "CONCURRENCY_ERROR");
        }
    }

    public static final class AlreadyRedeemed extends UsageException {
        public AlreadyRedeemed(String couponCode, String userId) {
            super(
                    String.format("Coupon %s has already been redeemed by user %s", couponCode, userId),
                    "USER_LIMIT_EXCEEDED"
            );
        }
    }

    public static final class InvalidCountry extends UsageException {
        // Version A: Simple (matching your service call)
        public InvalidCountry(String couponCode) {
            super("Coupon " + couponCode + " is not available in your location", "INVALID_COUNTRY");
        }

        // Version B: Detailed (if you want to show actual/expected)
        public InvalidCountry(String actual, String expected) {
            super(String.format("Invalid country: %s. This coupon is restricted to: %s", actual, expected), "INVALID_COUNTRY");
        }
    }

    public static final class NotFound extends UsageException {
        public NotFound(String couponCode) {
            super("Coupon not found: " + couponCode, "NOT_FOUND");
        }
    }


    public static final class RemoteServiceError extends UsageException {
        public RemoteServiceError(String message, String errorCode) {
            super(message, errorCode); // errorCode to np. "LIMIT_EXCEEDED"
        }
    }
}