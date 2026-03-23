package com.redemption.usage.integration;

import com.redemption.usage.application.UsageApplicationService;
import com.redemption.usage.domain.exception.UsageException;
import com.redemption.usage.domain.repository.UsageHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test focusing on the Retry mechanism.
 * We override Circuit Breaker properties to prevent it from opening
 * during retry attempts, ensuring we test only the Retry logic.
 */
@SpringBootTest(properties = {
        // Increase thresholds so the Circuit Breaker doesn't trip and trigger the fallback
        // during our Retry test iterations.
        "resilience4j.circuitbreaker.instances.internalServiceCB.failureRateThreshold=100",
        "resilience4j.circuitbreaker.instances.internalServiceCB.slidingWindowSize=20"
})
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
class UsageRetryFailureIntegrationTest {

    @Autowired
    private UsageApplicationService usageService;

    @Autowired
    private UsageHistoryRepository repository;

    @BeforeEach
    void setUp() {
        // Reset WireMock and Database state before each test
        reset();
        repository.deleteAll();

        // Mock GeoIP success to prevent the flow from breaking before reaching the coupon validation
        stubFor(get(urlMatching("/.*json/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"country\": \"PL\"}")));
    }

    @Test
    @DisplayName("Retry: Should exhaust all 3 attempts and throw ConcurrencyConflict")
    void shouldExhaustRetriesAndThrowException() {
        // GIVEN
        String code = "RETRY-TEST-CODE";
        String ip = "10.20.30.40";

        // WireMock: Always return a 200 OK with a CONCURRENCY_ERROR body.
        // This specific error should trigger the Retry logic defined in the service.
        stubFor(post(urlPathEqualTo("/api/v1/internal/coupons/" + code + "/validate-and-increment"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"success\": false, \"errorCode\": \"CONCURRENCY_ERROR\", \"message\": \"Conflict detected\"}")));

        // WHEN & THEN
        // We expect the original ConcurrencyConflict exception after all retries fail.
        // If the Circuit Breaker were to open, we would get RemoteServiceError instead (fallback).
        assertThatThrownBy(() -> usageService.redeem(code, ip))
                .isExactlyInstanceOf(UsageException.ConcurrencyConflict.class);

        // VERIFY: Based on 'maxAttempts: 3' in YAML, we expect exactly 3 calls (1 initial + 2 retries)
        verify(3, postRequestedFor(urlPathEqualTo("/api/v1/internal/coupons/" + code + "/validate-and-increment")));

        // VERIFY: Ensure no history was saved because the operation ultimately failed
        assertThat(repository.existsByCouponCodeAndUserId(code, ip)).isFalse();
    }
}