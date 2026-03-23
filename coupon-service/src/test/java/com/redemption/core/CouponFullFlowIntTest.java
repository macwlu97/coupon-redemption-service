package com.redemption.core;

import com.redemption.core.api.rest.dto.CouponResponse;
import com.redemption.core.api.rest.dto.CreateCouponRequest;
import com.redemption.core.domain.model.Coupon;
import com.redemption.core.domain.repository.CouponRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full Flow Integration Test (E2E).
 * Verifies the entire chain from REST Controller to Database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CouponFullFlowIntTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CouponRepository couponRepository;

    @Test
    @DisplayName("Full Flow: Should create a coupon via API and verify it exists in DB")
    void shouldCreateAndRetrieveCoupon() {
        // 1. GIVEN: A valid request to create a coupon
        CreateCouponRequest request = new CreateCouponRequest("BLACK-FRIDAY", 50, "PL");

        // 2. WHEN: Sending POST request to the API
        ResponseEntity<CouponResponse> createResponse = restTemplate.postForEntity(
                "/api/v1/coupons",
                request,
                CouponResponse.class
        );

        // 3. THEN: Check API Response
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        assertThat(createResponse.getBody().code()).isEqualTo("BLACK-FRIDAY");

        // 4. AND: Verify the data is actually in the database
        Coupon savedEntity = couponRepository.findByCode("BLACK-FRIDAY")
                .orElseThrow(() -> new AssertionError("Coupon not found in database!"));

        assertThat(savedEntity.getUsageLimit()).isEqualTo(50);
        assertThat(savedEntity.getTargetCountry()).isEqualTo("PL");
        assertThat(savedEntity.getCurrentUsage()).isZero();
    }

    @Test
    @DisplayName("Full Flow: Should fail when creating a coupon with invalid data (Validation)")
    void shouldFailOnInvalidData() {
        // GIVEN: Invalid request (negative usage limit)
        // Assuming @Min(1) is present in CreateCouponRequest
        CreateCouponRequest invalidRequest = new CreateCouponRequest("FAIL", -10, "PL");

        // WHEN
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/coupons",
                invalidRequest,
                String.class
        );

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
