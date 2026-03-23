package com.redemption.core.api.v1.internal;

import com.redemption.core.api.internal.InternalCouponController;
import com.redemption.core.api.internal.dto.CouponInternalResponse;
import com.redemption.core.application.CouponApplicationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web layer test for the Internal API.
 * Verifies that inter-service communication parameters are correctly mapped.
 */
@WebMvcTest(InternalCouponController.class)
class InternalCouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CouponApplicationService couponService;

    @Test
    @DisplayName("POST /validate-and-increment: Should return success response from service")
    void shouldReturnInternalResponseOnSuccess() throws Exception {
        // GIVEN
        String code = "SUMMER2026";
        String country = "PL";
        CouponInternalResponse expectedResponse = new CouponInternalResponse(true, null, "Validated");

        // We verify if Controller passes 'code' and 'countryCode' correctly to the service
        when(couponService.processInternalRedemption(eq(code), eq(country)))
                .thenReturn(expectedResponse);

        // WHEN & THEN
        mockMvc.perform(post("/api/v1/internal/coupons/{code}/validate-and-increment", code)
                        .param("countryCode", country) // Matches @RequestParam String countryCode
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Validated"))
                .andExpect(jsonPath("$.errorCode").isEmpty());
    }

    @Test
    @DisplayName("POST /validate-and-increment: Should return business error when validation fails")
    void shouldReturnErrorResponseWhenValidationFails() throws Exception {
        // GIVEN
        String code = "EXPIRED-CODE";
        CouponInternalResponse errorResponse = new CouponInternalResponse(false, "COUPON_EXPIRED", "Date exceeded");

        when(couponService.processInternalRedemption(eq(code), anyString()))
                .thenReturn(errorResponse);

        // WHEN & THEN
        mockMvc.perform(post("/api/v1/internal/coupons/{code}/validate-and-increment", code)
                        .param("countryCode", "US")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // Internal API returns 200, but logic is in the body
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("COUPON_EXPIRED"));
    }
}