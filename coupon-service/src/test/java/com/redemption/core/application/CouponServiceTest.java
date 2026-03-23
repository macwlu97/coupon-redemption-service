package com.redemption.core.application;

import com.redemption.core.api.internal.dto.CouponInternalResponse;
import com.redemption.core.domain.model.Coupon;
import com.redemption.core.domain.repository.CouponRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CouponApplicationService focusing on redemption flow and error handling.
 * All business exceptions are caught by the service and returned as Failure DTOs.
 */
@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponApplicationService couponService;

    @Test
    @DisplayName("Should return success response when redemption satisfies all business rules")
    void shouldReturnSuccessResponse() {
        // Given
        String code = "SAVE20";
        String country = "PL";
        Coupon coupon = new Coupon(code, 10, country);

        when(couponRepository.findByCode(code.toUpperCase())).thenReturn(Optional.of(coupon));

        // When
        CouponInternalResponse response = couponService.processInternalRedemption(code, country);

        // Then
        assertThat(response.success()).isTrue();
        assertThat(coupon.getCurrentUsage()).isEqualTo(1);
        verify(couponRepository, times(1)).save(coupon);
    }

    @Test
    @DisplayName("Should return NOT_FOUND error when coupon code does not exist in database")
    void shouldReturnFailureWhenNotFound() {
        // Given
        String code = "MISSING";
        when(couponRepository.findByCode(anyString())).thenReturn(Optional.empty());

        // When
        CouponInternalResponse response = couponService.processInternalRedemption(code, "PL");

        // Then
        assertThat(response.success()).isFalse();
        assertThat(response.errorCode()).isEqualTo("NOT_FOUND");
        verify(couponRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return INVALID_COUNTRY error when user country does not match coupon restriction")
    void shouldReturnFailureWhenCountryMismatch() {
        // Given
        String code = "UK-ONLY";
        Coupon coupon = new Coupon(code, 5, "GB"); // Restricted to GB

        when(couponRepository.findByCode(code.toUpperCase())).thenReturn(Optional.of(coupon));

        // When
        // User is from PL, but coupon is for GB
        CouponInternalResponse response = couponService.processInternalRedemption(code, "PL");

        // Then
        assertThat(response.success()).isFalse();
        assertThat(response.errorCode()).isEqualTo("INVALID_COUNTRY");

        // Ensure state was not changed and no save occurred
        assertThat(coupon.getCurrentUsage()).isZero();
        verify(couponRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return CONCURRENCY_ERROR when optimistic locking failure occurs")
    void shouldReturnFailureOnConcurrencyConflict() {
        // Given
        String code = "FLASH-SALE";
        Coupon coupon = new Coupon(code, 100, "PL");

        when(couponRepository.findByCode(anyString())).thenReturn(Optional.of(coupon));

        // Simulate Hibernate/JPA throwing an Optimistic Locking exception on save
        doThrow(new ObjectOptimisticLockingFailureException(Coupon.class, code))
                .when(couponRepository).save(any());

        // When
        CouponInternalResponse response = couponService.processInternalRedemption(code, "PL");

        // Then
        assertThat(response.success()).isFalse();
        assertThat(response.errorCode()).isEqualTo("CONCURRENCY_ERROR");
    }
}