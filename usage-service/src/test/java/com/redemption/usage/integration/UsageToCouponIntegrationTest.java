package com.redemption.usage.integration;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.redemption.usage.application.UsageApplicationService;
import com.redemption.usage.domain.exception.UsageException;
import com.redemption.usage.domain.repository.UsageHistoryRepository;
import com.redemption.usage.infrastructure.external.GeoIpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.cloud.config.import-check.enabled=false",
                "spring.config.import=",
                "coupon-service.url=http://localhost:${wiremock.server.port}",
                "spring.cloud.discovery.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.sql.init.mode=never"
        }
)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UsageToCouponIntegrationTest {

    @Autowired
    private UsageApplicationService usageService;

    @Autowired
    private UsageHistoryRepository repository;

    @MockitoBean
    private GeoIpService geoIpService;

    @BeforeEach
    void setup() {
        repository.deleteAll();
        WireMock.reset();

        // Prevent 429 errors from real GeoIP service during tests
        given(geoIpService.getCountryCode(anyString())).willReturn("PL");

        // Default Global Stub - SUCCESS (Low Priority - 5)
        stubFor(post(urlPathMatching("/api/v1/internal/coupons/.*/validate-and-increment.*"))
                .atPriority(5)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"success\": true, \"message\": \"OK\"}")));
    }

    @Test
    @DisplayName("Redeem: Should fail with AlreadyRedeemed on second attempt (Idempotency)")
    void shouldFailWhenAlreadyRedeemedLocally() {
        String code = "SINGLE-USE";
        String ip = "9.9.9.9";

        // First attempt - SUCCESS
        usageService.redeem(code, ip);
        assertThat(repository.count()).isEqualTo(1);

        // Second attempt - MUST throw AlreadyRedeemed locally
        assertThatThrownBy(() -> usageService.redeem(code, ip))
                .isExactlyInstanceOf(UsageException.AlreadyRedeemed.class);
    }

    @Test
    @DisplayName("Resilience: Should retry on CONCURRENCY_ERROR and throw ConcurrencyConflict")
    void shouldHandleConcurrencyError() {
        // Specific Stub - CONFLICT (High Priority - 1)
        // High priority ensures WireMock picks this stub over the global default in BeforeEach
        stubFor(post(urlPathMatching("/api/v1/internal/coupons/.*/validate-and-increment.*"))
                .atPriority(1)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"success\": false, \"errorCode\": \"CONCURRENCY_ERROR\", \"message\": \"Conflict occurred\"}")));

        // The assertion ensures the final result after retries is still ConcurrencyConflict
        assertThatThrownBy(() -> usageService.redeem("RETRY-CODE", "1.1.1.1"))
                .isExactlyInstanceOf(UsageException.ConcurrencyConflict.class);

        // Verify that Retry mechanism actually worked (called 3 times)
        verify(3, postRequestedFor(urlPathMatching("/api/v1/internal/coupons/RETRY-CODE/validate-and-increment.*")));
    }
}