package com.redemption.usage.integration;

import com.redemption.usage.application.UsageApplicationService;
import com.redemption.usage.domain.repository.UsageHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.config.import=",
                "spring.cloud.config.fail-fast=false",
                "spring.datasource.url=jdbc:h2:mem:usage_test_db;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.jpa.hibernate.ddl-auto=create-drop"
        }
)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
class UsageRetryIntegrationTest {

    @Autowired
    private UsageApplicationService usageService;

    @Autowired
    private UsageHistoryRepository repository;

    @Test
    void shouldSucceedOnThirdAttemptAfterConcurrencyErrors() {
        // Given
        String code = "RETRY-WINNER";
        String ip = "1.2.3.4";

        // WireMock Scenario: 1st call -> Error, 2nd call -> Error, 3rd call -> Success
        stubFor(post(urlPathMatching("/api/v1/internal/coupons/.*/validate-and-increment.*"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"success\": false, \"errorCode\": \"CONCURRENCY_ERROR\"}"))
                .willSetStateTo("First Failure"));

        stubFor(post(urlPathMatching("/api/v1/internal/coupons/.*/validate-and-increment.*"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("First Failure")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"success\": false, \"errorCode\": \"CONCURRENCY_ERROR\"}"))
                .willSetStateTo("Second Failure"));

        stubFor(post(urlPathMatching("/api/v1/internal/coupons/.*/validate-and-increment.*"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Second Failure")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"success\": true, \"message\": \"Finally!\"}")));

        // When
        String result = usageService.redeem(code, ip);

        // Then
        assertThat(result).isNotNull();
        assertThat(repository.existsByCouponCodeAndUserId(code.toUpperCase(), ip)).isTrue();

        // Verify: WireMock should have received exactly 3 requests
        verify(3, postRequestedFor(urlPathMatching("/api/v1/internal/coupons/.*/validate-and-increment.*")));
    }
}