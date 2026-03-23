package com.redemption.usage.application;

import com.redemption.usage.domain.exception.UsageException;
import com.redemption.usage.domain.model.UsageHistory;
import com.redemption.usage.domain.repository.UsageHistoryRepository;
import com.redemption.usage.infrastructure.external.CouponServiceClient;
import com.redemption.usage.infrastructure.external.GeoIpService;
import com.redemption.usage.infrastructure.external.dto.CouponInternalResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsageApplicationServiceTest {

    @Mock
    private UsageHistoryRepository usageRepository;

    @Mock
    private CouponServiceClient couponClient;

    @Mock
    private GeoIpService geoIpService;

    @InjectMocks
    private UsageApplicationService usageService;

    @Test
    @DisplayName("Redeem: Success flow - should return country and save history")
    void shouldRedeemSuccessfully() {
        // GIVEN
        String code = "SUMMER24";
        String ip = "1.2.3.4";
        String country = "PL";

        when(usageRepository.existsByCouponCodeAndUserId(code, ip)).thenReturn(false);
        when(geoIpService.getCountryCode(ip)).thenReturn(country);
        when(couponClient.validateAndIncrement(code, country))
                .thenReturn(new CouponInternalResponse(true, null, "Success"));

        // WHEN
        String result = usageService.redeem(code, ip);

        // THEN
        assertThat(result).isEqualTo(country);
        verify(usageRepository).save(any(UsageHistory.class));
        verify(couponClient).validateAndIncrement(code, country);
    }

    @Test
    @DisplayName("Redeem: Should fail early if already redeemed (Idempotency)")
    void shouldFailWhenAlreadyRedeemed() {
        // GIVEN
        String code = "SUMMER24";
        String ip = "1.2.3.4";
        when(usageRepository.existsByCouponCodeAndUserId(code, ip)).thenReturn(true);

        // WHEN & THEN
        assertThatThrownBy(() -> usageService.redeem(code, ip))
                .isExactlyInstanceOf(UsageException.AlreadyRedeemed.class);

        // VERIFY: Ważne! Sprawdzamy, czy nie tracimy zasobów na zbędne wywołania API
        verifyNoInteractions(geoIpService);
        verifyNoInteractions(couponClient);
    }

    @Test
    @DisplayName("Redeem: Should throw ConcurrencyConflict when remote service returns CONCURRENCY_ERROR")
    void shouldThrowConcurrencyConflict() {
        // GIVEN
        String code = "LIMIT-1";
        String ip = "5.5.5.5";
        when(geoIpService.getCountryCode(ip)).thenReturn("US");
        when(couponClient.validateAndIncrement(anyString(), anyString()))
                .thenReturn(new CouponInternalResponse(false, "CONCURRENCY_ERROR", "Conflict"));

        // WHEN & THEN
        assertThatThrownBy(() -> usageService.redeem(code, ip))
                .isExactlyInstanceOf(UsageException.ConcurrencyConflict.class);
    }

    @Test
    @DisplayName("Redeem: Should handle unexpected remote service error")
    void shouldHandleRemoteError() {
        // GIVEN
        when(geoIpService.getCountryCode(anyString())).thenReturn("FR");
        when(couponClient.validateAndIncrement(anyString(), anyString()))
                .thenReturn(new CouponInternalResponse(false, "UNKNOWN_ERROR", "Something went wrong"));

        // WHEN & THEN
        assertThatThrownBy(() -> usageService.redeem("CODE", "8.8.8.8"))
                .isExactlyInstanceOf(UsageException.RemoteServiceError.class);

        verify(usageRepository, never()).save(any());
    }
}