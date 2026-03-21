package com.redemption.core.infrastructure.advice;

import com.redemption.core.domain.exception.CouponException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles domain-specific business exceptions.
     */
    @ExceptionHandler(CouponException.class)
    public ProblemDetail handleCouponException(CouponException ex) {
        // Using Java 21 pattern matching for switch
        HttpStatus status = switch (ex) {
            case CouponException.NotFound ignored -> HttpStatus.NOT_FOUND;
            case CouponException.InvalidCountry ignored -> HttpStatus.BAD_REQUEST;
            case CouponException.LimitExceeded ignored -> HttpStatus.CONFLICT;
        };

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setTitle("Coupon Business Error");
        problem.setProperty("error_code", ex.getCode());
        return problem;
    }

    /**
     * Handles Concurrency issues (Optimistic Locking).
     * This happens when two users try to redeem the same coupon simultaneously.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleConcurrencyConflict(ObjectOptimisticLockingFailureException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "The coupon is currently being processed by another request. Please try again."
        );
        problem.setTitle("Concurrency Conflict");
        problem.setProperty("error_code", "CONCURRENT_UPDATE");
        // Good practice for Senior level: point to documentation or help
        problem.setType(URI.create("https://api.redemption.com/errors/concurrency"));
        return problem;
    }
}