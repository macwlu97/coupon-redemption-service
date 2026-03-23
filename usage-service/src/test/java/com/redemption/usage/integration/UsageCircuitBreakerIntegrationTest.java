package com.redemption.usage.integration;

import com.redemption.usage.application.UsageApplicationService;
import com.redemption.usage.domain.exception.UsageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
class UsageCircuitBreakerIntegrationTest {

    @Autowired
    private UsageApplicationService usageService;

    @BeforeEach
    void setUp() {
        reset();
        stubFor(get(urlMatching("/.*json/")).willReturn(aResponse().withStatus(200).withBody("{\"country\":\"PL\"}")));
    }

    @Test
    void shouldOpenCircuitAndFailFast() {
        // Given
        stubFor(post(urlPathMatching("/api/v1/internal/coupons/CB-TEST/.*"))
                .willReturn(aResponse().withStatus(500)));

        // When - triggering CB (window size 10 in your YAML)
        for (int i = 0; i < 15; i++) {
            try {
                usageService.redeem("CB-TEST", "user-1");
            } catch (Exception ignored) {}
        }

        // Then - Should fail fast
        assertThatThrownBy(() -> usageService.redeem("CB-TEST", "user-1"))
                .isInstanceOf(UsageException.RemoteServiceError.class);

        // Verify - should not reach 45 (15 * 3 retries) due to CB
        verify(moreThanOrExactly(10), postRequestedFor(urlPathMatching("/api/v1/internal/coupons/CB-TEST/.*")));
        verify(lessThanOrExactly(20), postRequestedFor(urlPathMatching("/api/v1/internal/coupons/CB-TEST/.*")));
    }
}