//package com.redemption.core.domain.model;
//
//import com.redemption.core.domain.exception.CouponException; // Poprawiony import
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//
//class CouponTest {
//
//    @Test
//    @DisplayName("Should successfully redeem coupon when all conditions are met")
//    void shouldRedeemCoupon() {
//        // Given
//        Coupon coupon = new Coupon("PROMO2026", 100, "PL");
//
//        // When
//        coupon.redeem("PL");
//
//        // Then
//        assertThat(coupon.getCurrentUsage()).isEqualTo(1);
//    }
//
//    @Test
//    @DisplayName("Should throw exception when country is invalid")
//    void shouldThrowExceptionWhenCountryIsInvalid() {
//        // Given
//        Coupon coupon = new Coupon("POLSKA_ONLY", 100, "PL");
//
//        // When & Then
//        assertThatThrownBy(() -> coupon.redeem("DE"))
//                .isInstanceOf(CouponException.InvalidCountry.class); // Dopasowane do encji
//    }
//
//    @Test
//    @DisplayName("Should throw exception when usage limit is reached")
//    void shouldThrowExceptionWhenLimitReached() {
//        // Given
//        // Creating a coupon with a usage limit of 1.
//        Coupon coupon = new Coupon("LIMITED", 1, "PL");
//        // Using it once.
//        coupon.redeem("PL");
//
//        // When & Then
//        assertThatThrownBy(() -> coupon.redeem("PL"))
//                .isInstanceOf(CouponException.LimitExceeded.class); // Mark it as used once.
//    }
//
//    @Test
//    @DisplayName("Should be case-insensitive for country check")
//    void shouldBeCaseInsensitiveForCountry() {
//        // Given
//        Coupon coupon = new Coupon("TEST", 10, "PL");
//
//        // When
//        coupon.redeem("pl");
//
//        // Then
//        assertThat(coupon.getCurrentUsage()).isEqualTo(1);
//    }
//}