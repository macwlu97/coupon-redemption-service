//package com.redemption.core.api.v1;
//
//import com.redemption.core.domain.model.Coupon;
//import com.redemption.core.domain.repository.CouponRepository;
//import com.redemption.core.infrastructure.external.GeoIpClient;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.assertj.core.api.Assertions.assertThat;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//class CouponIntegrationTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private CouponRepository couponRepository;
//
//    @MockitoBean
//    private GeoIpClient geoIpClient;
//
//    @BeforeEach
//    void setUp() {
//        couponRepository.deleteAll();
//    }
//
//    @Test
//    @DisplayName("Redeem Flow: Should successfully redeem coupon using X-Forwarded-For header")
//    void shouldRedeemCouponSuccessfully() throws Exception {
//        String code = "WIOSNA2026";
//        couponRepository.save(new Coupon(code, 100, "PL"));
//
//        // Use anyString() to match any IP format passed by MockMvc
//        given(geoIpClient.fetchCountryCode(anyString())).willReturn("PL");
//
//        mockMvc.perform(post("/api/v1/coupons/{code}/redeem", code)
//                        .header("X-Forwarded-For", "5.173.0.1"))
//                .andExpect(status().isOk());
//
//        Coupon updatedCoupon = couponRepository.findByCode(code).orElseThrow();
//        assertThat(updatedCoupon.getCurrentUsage()).isEqualTo(1);
//    }
//
//    @Test
//    @DisplayName("Redeem Flow: Should return 400 when geo-restriction is violated")
//    void shouldReturnBadRequestForInvalidCountry() throws Exception {
//        String code = "WIOSNA2026";
//        couponRepository.save(new Coupon(code, 100, "PL"));
//
//        // Mocking geo-service to return DE for any IP in this test
//        given(geoIpClient.fetchCountryCode(anyString())).willReturn("DE");
//
//        mockMvc.perform(post("/api/v1/coupons/{code}/redeem", code)
//                        .header("X-Forwarded-For", "3.120.0.1"))
//                .andExpect(status().isBadRequest()); // This matches HttpStatus.BAD_REQUEST (400)
//    }
//}