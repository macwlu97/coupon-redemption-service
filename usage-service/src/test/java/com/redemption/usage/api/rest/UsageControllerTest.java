package com.redemption.usage.api.rest;

import com.redemption.usage.application.UsageApplicationService;
import com.redemption.usage.domain.exception.UsageException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean; // New import for Spring 3.4+
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web layer test for UsageController.
 * Updated to use @MockitoBean as @MockBean is deprecated in Spring Boot 3.4+.
 */
@WebMvcTest(UsageController.class)
class UsageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Use @MockitoBean instead of @MockBean
    @MockitoBean
    private UsageApplicationService usageService;

    @Test
    @DisplayName("POST /redeem: Should return 200 OK and usage details on success")
    void shouldReturnOkOnSuccess() throws Exception {
        // GIVEN
        String code = "SUMMER2026";
        String country = "PL";
        when(usageService.redeem(anyString(), anyString())).thenReturn(country);

        // WHEN & THEN
        mockMvc.perform(post("/api/v1/usages/{code}/redeem", code)
                        .header("X-Forwarded-For", "1.2.3.4")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // $.couponCode -> $.code:
                .andExpect(jsonPath("$.code").value(code))
                // $.country -> $.detectedCountry:
                .andExpect(jsonPath("$.detectedCountry").value(country))
                .andExpect(jsonPath("$.redeemedAt").exists());
    }

    @Test
    @DisplayName("POST /redeem: Should return 409 Conflict on AlreadyRedeemed exception")
    void shouldReturnConflictWhenAlreadyRedeemed() throws Exception {
        // GIVEN
        when(usageService.redeem(anyString(), anyString()))
                .thenThrow(new UsageException.AlreadyRedeemed("CODE", "IP"));

        // WHEN & THEN
        mockMvc.perform(post("/api/v1/usages/CODE/redeem")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }
}