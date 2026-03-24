package com.redemption.usage.infrastructure.external;

import com.redemption.usage.infrastructure.external.provider.GeoIpProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Integration test for GeoIpService resilience patterns.
 * We use ReflectionTestUtils to inject mocks into the Spring-managed Proxy.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
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
class GeoIpServiceResilienceTest {

    @Autowired
    private GeoIpService geoIpService;

    @MockitoBean(name = "primaryProvider")
    private GeoIpProvider primaryProvider;

    @MockitoBean(name = "secondaryProvider")
    private GeoIpProvider secondaryProvider;

    @BeforeEach
    void setUp() {
        // Essential: Inject our mocks into the Spring Bean's private 'providers' list.
        // This ensures the Proxy uses our mocks while keeping Resilience4j annotations active.
        ReflectionTestUtils.setField(geoIpService, "providers",
                List.of(primaryProvider, secondaryProvider));

        // Define priorities for the Strategy pattern
        when(primaryProvider.getPriority()).thenReturn(1);
        when(secondaryProvider.getPriority()).thenReturn(2);
    }

    @Test
    @DisplayName("Should trigger fallback and use secondary provider when primary fails")
    void shouldFailoverToSecondaryProvider() {
        // Given
        String testIp = "8.8.8.8";

        // Primary fails - this should trigger the @CircuitBreaker fallback method
        when(primaryProvider.fetchCountryCode(testIp))
                .thenThrow(new RuntimeException("Primary service down"));

        // Secondary succeeds
        when(secondaryProvider.fetchCountryCode(testIp))
                .thenReturn("US");

        // When
        String result = geoIpService.getCountryCode(testIp);

        // Then
        assertThat(result)
                .as("The service should catch the exception and switch to the secondary provider")
                .isEqualTo("US");

        verify(primaryProvider, atLeastOnce()).fetchCountryCode(testIp);
        verify(secondaryProvider).fetchCountryCode(testIp);
    }

    @Test
    @DisplayName("Should return 'UNKNOWN' when all providers are failing")
    void shouldReturnUnknownWhenAllProvidersFail() {
        // Given
        String testIp = "1.1.1.1";
        when(primaryProvider.fetchCountryCode(anyString())).thenThrow(new RuntimeException("Down"));
        when(secondaryProvider.fetchCountryCode(anyString())).thenThrow(new RuntimeException("Also Down"));

        // When
        String result = geoIpService.getCountryCode(testIp);

        // Then
        assertThat(result).isEqualTo("UNKNOWN");
    }
}