//package com.redemption.core.application;
//
//import com.redemption.core.domain.model.Coupon;
//import com.redemption.core.domain.repository.CouponRepository;
//import com.redemption.core.domain.exception.CouponException;
//import com.redemption.core.infrastructure.external.GeoIpClient;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class) // Enables Mockito support for JUnit 5
//class CouponServiceTest {
//
//    @Mock
//    private CouponRepository couponRepository; // Mocked database dependency
//
//    @Mock
//    private GeoIpClient geoIpClient; // Mocked external API dependency
//
//    @InjectMocks
//    private CouponApplicationService couponService; // Service with all mocks injected
//
//    @Test
//    @DisplayName("Should successfully process redemption when country and limits are valid")
//    void shouldProcessRedemptionFlow() {
//        // Given
//        String code = "SAVE10";
//        String ip = "1.2.3.4";
//        String countryCode = "US";
//        Coupon coupon = new Coupon(code, 10, countryCode);
//
//        // Mocking behavior: GeoIP returns US, and DB finds the coupon
//        when(geoIpClient.fetchCountryCode(ip)).thenReturn(countryCode);
//        when(couponRepository.findByCode(code.toUpperCase())).thenReturn(Optional.of(coupon));
//
//        // When
//        couponService.processRedemption(code, ip); // Calling the correct method name
//
//        // Then
//        assertThat(coupon.getCurrentUsage()).isEqualTo(1);
//
//        // Verify that the repository's save method was called with updated coupon
//        verify(couponRepository, times(1)).save(coupon);
//    }
//
//    @Test
//    @DisplayName("Should throw NotFound exception when coupon code does not exist")
//    void shouldHandleMissingCoupon() {
//        // Given
//        String code = "FAKE";
//        String ip = "1.2.3.4";
//
//        when(geoIpClient.fetchCountryCode(ip)).thenReturn("PL");
//        when(couponRepository.findByCode(code.toUpperCase())).thenReturn(Optional.empty());
//
//        // When & Then
//        assertThatThrownBy(() -> couponService.processRedemption(code, ip))
//                .isInstanceOf(CouponException.NotFound.class);
//
//        // Verify that save was never called
//        verify(couponRepository, never()).save(any());
//    }
//
//    @Test
//    @DisplayName("Should throw InvalidCountry exception when GeoIP returns different country")
//    void shouldFailWhenCountryMismatch() {
//        // Given
//        String code = "POLAND-ONLY";
//        String ip = "8.8.8.8"; // Foreign IP
//        Coupon coupon = new Coupon(code, 10, "PL");
//
//        when(geoIpClient.fetchCountryCode(ip)).thenReturn("US"); // Mocking US location
//        when(couponRepository.findByCode(code.toUpperCase())).thenReturn(Optional.of(coupon));
//
//        // When & Then
//        assertThatThrownBy(() -> couponService.processRedemption(code, ip))
//                .isInstanceOf(CouponException.InvalidCountry.class);
//
//        // Ensure state wasn't saved after validation failure
//        verify(couponRepository, never()).save(any());
//    }
//}