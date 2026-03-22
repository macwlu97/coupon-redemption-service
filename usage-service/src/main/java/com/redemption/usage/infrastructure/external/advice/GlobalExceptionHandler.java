package com.redemption.usage.infrastructure.external.advice;

import com.redemption.usage.domain.exception.UsageException;
import feign.FeignException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ObjectMapper objectMapper;

    /**
     * Handles Feign communication errors.
     * Uses httpMethod() to avoid deprecated method() call.
     */
    @ExceptionHandler(FeignException.class)
    public ProblemDetail handleFeignException(FeignException ex) {
        // Safe access to HTTP method name using Java 21 style null-checks
        String methodName = (ex.request() != null) ? ex.request().httpMethod().name() : "UNKNOWN";
        log.error("Downstream service error: status {}, method {}", ex.status(), methodName);

        HttpStatus status = HttpStatus.resolve(ex.status());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "External service communication failure");

        try {
            String content = ex.contentUTF8();
            if (content != null && !content.isEmpty()) {
                // Mapping remote error details to local ProblemDetail
                Map<String, Object> errorDetails = objectMapper.readValue(content, Map.class);
                problem.setDetail((String) errorDetails.getOrDefault("detail", ex.getMessage()));
                problem.setProperty("remote_error_code", errorDetails.get("error_code"));
            }
        } catch (Exception e) {
            log.warn("Failed to parse Feign error body: {}", e.getMessage());
        }

        problem.setTitle("Proxy Error");
        return problem;
    }

    @ExceptionHandler(UsageException.class)
    public ProblemDetail handleUsageException(UsageException ex) {
        HttpStatus status = switch (ex) {
            case UsageException.AlreadyRedeemed ignored -> HttpStatus.CONFLICT; // 409
            case UsageException.RemoteServiceError remoteErr -> switch (remoteErr.getErrorCode()) {
                case "NOT_FOUND" -> HttpStatus.NOT_FOUND;          // 404
                case "LIMIT_EXCEEDED" -> HttpStatus.CONFLICT;      // 409
                case "INVALID_COUNTRY" -> HttpStatus.BAD_REQUEST;  // 400
                default -> HttpStatus.UNPROCESSABLE_ENTITY;        // 422
            };
        };

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setTitle("Usage Business Rule Violation");
        problem.setProperty("error_code", ex.getErrorCode());

        return problem;
    }

    /**
     * Handles local business logic errors (e.g., from UsageApplicationService).
     */
    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail handleRuntimeException(RuntimeException ex) {
        log.error("Internal application error", ex);

        // Simple mapping for demonstration; in production use custom exceptions like in coupon-service
        HttpStatus status = ex.getMessage().contains("already used") ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setTitle("Usage Service Error");
        return problem;
    }

    /**
     * Final fallback for unexpected exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneralException(Exception ex) {
        log.error("Unexpected system failure", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setTitle("System Error");
        return problem;
    }
}