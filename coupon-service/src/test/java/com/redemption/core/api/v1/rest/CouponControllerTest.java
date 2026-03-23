package com.redemption.core.api.v1.rest;

import com.redemption.core.api.rest.CouponController;
import com.redemption.core.api.rest.dto.CouponResponse;
import com.redemption.core.api.rest.dto.CreateCouponRequest;
import com.redemption.core.application.CouponApplicationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web layer integration test for CouponController.
 * Uses @MockitoBean (Spring Boot 3.4+) to mock the application service.
 */
@WebMvcTest(CouponController.class)
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CouponApplicationService couponService;

    @Test
    @DisplayName("POST /api/v1/coupons: Should create coupon and return 201 Created")
    void shouldCreateCoupon() throws Exception {
        // GIVEN
        String jsonRequest = """
                {
                    "code": "SUMMER2026",
                    "usageLimit": 100,
                    "targetCountry": "PL"
                }
                """;

        // Match the exact constructor of your CouponResponse record
        CouponResponse response = new CouponResponse(
                "SUMMER2026",
                100,
                0,
                "PL",
                LocalDateTime.now()
        );

        when(couponService.createCoupon(any(CreateCouponRequest.class))).thenReturn(response);

        // WHEN & THEN
        mockMvc.perform(post("/api/v1/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUMMER2026"))
                .andExpect(jsonPath("$.usageLimit").value(100))
                .andExpect(jsonPath("$.currentUsage").value(0))
                .andExpect(jsonPath("$.targetCountry").value("PL"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("GET /api/v1/coupons: Should return paginated list of coupons")
    void shouldReturnPaginatedCoupons() throws Exception {
        // GIVEN
        CouponResponse coupon = new CouponResponse(
                "WINTER2026",
                50,
                10,
                "DE",
                LocalDateTime.now()
        );

        // Spring Data Page implementation for testing
        Page<CouponResponse> page = new PageImpl<>(List.of(coupon), PageRequest.of(0, 20), 1);

        when(couponService.findAll(any())).thenReturn(page);

        // WHEN & THEN
        mockMvc.perform(get("/api/v1/coupons")
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("WINTER2026"))
                .andExpect(jsonPath("$.content[0].usageLimit").value(50))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/coupons: Should return 400 Bad Request on invalid input")
    void shouldReturnBadRequestOnInvalidInput() throws Exception {
        // GIVEN: Assuming CreateCouponRequest has @NotBlank and @Min(1)
        String invalidRequest = """
                {
                    "code": "",
                    "usageLimit": -1
                }
                """;

        // WHEN & THEN
        mockMvc.perform(post("/api/v1/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }
}