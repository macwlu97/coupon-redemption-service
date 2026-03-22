//package com.redemption.core.infrastructure.external;
//
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
//import org.springframework.web.client.RestClientException;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.Mockito.*;
//
///**
// * Test class focused on verifying the Resilience4j Retry and Fallback logic.
// * Uses @MockitoSpyBean to wrap the real bean and monitor retry attempts.
// */
//@SpringBootTest
//class GeoIpClientRetryTest {
//
//    @MockitoSpyBean
//    private GeoIpClient geoIpClient;
//
//    @Test
//    @DisplayName("Should retry 3 times when the external GeoIP service throws an exception")
//    void shouldAttemptRetryThreeTimesOnNetworkFailure() {
//        // Given
//        String testIp = "8.8.8.8";
//
//        // Force the call to fail with an exception to trigger @Retry
//        doThrow(new RestClientException("Remote service is down"))
//                .when(geoIpClient).fetchCountryCode(testIp);
//
//        // When
//        try {
//            geoIpClient.fetchCountryCode(testIp);
//        } catch (Exception ignored) {
//            // Expected failure after all retry attempts
//        }
//
//        // Then
//        // Resilience4j mechanism should trigger the method 3 times (1 initial + 2 retries)
//        // based on the configuration in your application.yml
//        verify(geoIpClient, times(3)).fetchCountryCode(testIp);
//    }
//
//    @Test
//    @DisplayName("Should return 'UNKNOWN' as a fallback when all retries are exhausted")
//    void shouldReturnFallbackValueWhenServiceIsPermanentlyUnavailable() {
//        // Given
//        String testIp = "1.1.1.1";
//
//        // Simulating permanent failure
//        doThrow(new RestClientException("Critical failure"))
//                .when(geoIpClient).fetchCountryCode(testIp);
//
//        // When
//        String result = geoIpClient.fetchCountryCode(testIp);
//
//        // Then
//        // The fallbackMethod "fallbackCountry" defined in @Retry should be executed
//        assertThat(result).isEqualTo("UNKNOWN");
//    }
//}
