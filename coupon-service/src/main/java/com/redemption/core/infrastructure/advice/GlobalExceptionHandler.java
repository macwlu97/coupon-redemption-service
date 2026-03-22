package com.redemption.core.infrastructure.advice;

import com.redemption.core.domain.exception.CouponException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles domain business exceptions using Java 21 Pattern Matching.
     */
    @ExceptionHandler(CouponException.class)
    public ProblemDetail handleCouponException(CouponException ex) {
        HttpStatus status = switch (ex) {
            case CouponException.NotFound ignored -> HttpStatus.NOT_FOUND;
            case CouponException.InvalidCountry ignored -> HttpStatus.BAD_REQUEST;
            case CouponException.LimitExceeded ignored -> HttpStatus.CONFLICT;
            case CouponException.AlreadyExists ignored -> HttpStatus.CONFLICT;
        };

        var problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setTitle("Coupon Business Rule Violation");
        problem.setProperty("error_code", ex.getCode());
        return problem;
    }

    /**
     * Handles Concurrency issues (Optimistic Locking).
     * Vital for high-scalability systems where multiple nodes update the same record.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleConcurrencyConflict(ObjectOptimisticLockingFailureException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "The coupon is being updated by another request. Please retry."
        );
        problem.setTitle("Concurrency Conflict");
        problem.setProperty("error_code", "CONCURRENT_UPDATE");
        return problem;
    }
}