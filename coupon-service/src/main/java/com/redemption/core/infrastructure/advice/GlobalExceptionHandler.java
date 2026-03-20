package com.redemption.core.infrastructure.advice;

import com.redemption.core.domain.exception.CouponException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CouponException.class)
    public ProblemDetail handleCouponException(CouponException ex) {
        // Java 21 Pattern Matching for switch
        HttpStatus status = switch (ex) {
            case CouponException.NotFound ignored-> HttpStatus.NOT_FOUND;
            case CouponException.InvalidCountry ignored -> HttpStatus.FORBIDDEN;
            case CouponException.LimitExceeded ignored -> HttpStatus.CONFLICT;
        };

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setTitle("Coupon Business Error");
        problem.setProperty("error_code", ex.getCode());
        return problem;
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleConcurrency() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "The system is currently busy. Please try again in a moment."
        );
        problem.setTitle("Concurrency Conflict");
        return problem;
    }
}