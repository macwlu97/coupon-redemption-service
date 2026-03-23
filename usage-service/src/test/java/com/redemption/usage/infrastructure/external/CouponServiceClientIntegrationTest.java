package com.redemption.usage.infrastructure.external;

import com.redemption.usage.infrastructure.external.dto.CouponInternalResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Feign Client using WireMock to simulate external service behavior.
 * This ensures that HTTP requests are correctly formed and JSON responses are properly
 * deserialized into Java DTOs.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWireMock(port = 0) // Starts WireMock on a random available port
@ActiveProfiles("test")
class CouponServiceClientIntegrationTest {

    @Autowired
    private CouponServiceClient couponServiceClient;

    @Test
    @DisplayName("Feign: Should correctly map a successful response from the external service")
    void shouldReturnSuccessResponseFromExternalService() {
        // GIVEN: Prepare mock data and external service stub
        String code = "SUMMER2026";
        String country = "PL";

        // Define WireMock behavior (Stubbing)
        stubFor(post(urlPathEqualTo("/api/v1/internal/coupons/" + code + "/validate-and-increment"))
                .withQueryParam("countryCode", equalTo(country))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                            {
                                "success": true,
                                "errorCode": null,
                                "message": "Coupon usage incremented successfully"
                            }
                            """)));

        // WHEN: Call the Feign client
        CouponInternalResponse response = couponServiceClient.validateAndIncrement(code, country);

        // THEN: Verify the response was correctly deserialized
        assertThat(response).isNotNull();
        assertThat(response.success()).isTrue();
        assertThat(response.message()).contains("successfully");
        assertThat(response.errorCode()).isNull();
    }

    @Test
    @DisplayName("Feign: Should correctly map CONCURRENCY_ERROR from the external service")
    void shouldReturnErrorResponseOnConflict() {
        // GIVEN: Stubbing a scenario where the external service returns a business error
        String code = "HOT-DEAL";
        String country = "PL";

        stubFor(post(urlPathEqualTo("/api/v1/internal/coupons/" + code + "/validate-and-increment"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                            {
                                "success": false,
                                "errorCode": "CONCURRENCY_ERROR",
                                "message": "Too many requests for this coupon"
                            }
                            """)));

        // WHEN: Call the Feign client
        CouponInternalResponse response = couponServiceClient.validateAndIncrement(code, country);

        // THEN: Verify the error code is correctly captured
        assertThat(response).isNotNull();
        assertThat(response.success()).isFalse();
        assertThat(response.errorCode()).isEqualTo("CONCURRENCY_ERROR");
        assertThat(response.message()).isEqualTo("Too many requests for this coupon");
    }
}