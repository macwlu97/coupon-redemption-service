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

    public static final class RemoteServiceError extends UsageException {
        public RemoteServiceError(String message, String errorCode) {
            super(message, errorCode); // errorCode to np. "LIMIT_EXCEEDED"
        }
    }
}