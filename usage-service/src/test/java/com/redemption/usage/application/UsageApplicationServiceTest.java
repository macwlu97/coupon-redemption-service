package com.redemption.usage.application;

import com.redemption.usage.domain.exception.UsageException;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsageApplicationServiceTest {

    @Mock
    private UsageHistoryRepository usageRepository;

    @Mock
    private UsageHistoryService historyService; // FIX: Added missing mock to prevent NPE

    @Mock
    private CouponServiceClient couponClient;

    @Mock
    private GeoIpService geoIpService;

    @InjectMocks
    private UsageApplicationService usageService;

    @Test
    @DisplayName("Redeem: Success flow - should return country and delegate save to historyService")
    void shouldRedeemSuccessfully() {
        // GIVEN
        String code = "summer24";
        String upperCode = "SUMMER24";
        String ip = "1.2.3.4";
        String country = "PL";

        // Mocking dependencies (Service converts code toUpperCase internally)
        when(usageRepository.existsByCouponCodeAndUserId(upperCode, ip)).thenReturn(false);
        when(geoIpService.getCountryCode(ip)).thenReturn(country);
        when(couponClient.validateAndIncrement(upperCode, country))
                .thenReturn(new CouponInternalResponse(true, null, "Success"));

        // WHEN
        String result = usageService.redeem(code, ip);

        // THEN
        assertThat(result).isEqualTo(country);

        // VERIFY: Check if the task was delegated to the new service
        verify(historyService).persistUsage(upperCode, ip);
        verify(couponClient).validateAndIncrement(upperCode, country);
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

        // VERIFY: Ensure no resources are wasted on external API calls
        verifyNoInteractions(geoIpService);
        verifyNoInteractions(couponClient);
        verifyNoInteractions(historyService);
    }

    @Test
    @DisplayName("Redeem: Should throw ConcurrencyConflict when remote service returns error")
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

        // Database should NOT be hit if remote validation fails
        verifyNoInteractions(historyService);
    }

    @Test
    @DisplayName("Redeem: Should handle unexpected remote service error")
    void shouldHandleRemoteError() {
        // GIVEN
        String code = "CODE";
        String ip = "8.8.8.8";
        when(geoIpService.getCountryCode(anyString())).thenReturn("FR");
        when(couponClient.validateAndIncrement(anyString(), anyString()))
                .thenReturn(new CouponInternalResponse(false, "UNKNOWN", "Error"));

        // WHEN & THEN
        assertThatThrownBy(() -> usageService.redeem(code, ip))
                .isExactlyInstanceOf(UsageException.RemoteServiceError.class);

        verifyNoInteractions(historyService);
    }

    @Test
    @DisplayName("Redeem: Should handle geofencing failure (InvalidCountry)")
    void shouldHandleGeofencingFailure() {
        // GIVEN
        String code = "PL-ONLY";
        String ip = "8.8.8.8"; // US IP
        when(geoIpService.getCountryCode(ip)).thenReturn("US");
        when(couponClient.validateAndIncrement(anyString(), anyString()))
                .thenReturn(new CouponInternalResponse(false, "INVALID_COUNTRY", "Wrong country"));

        // WHEN & THEN
        assertThatThrownBy(() -> usageService.redeem(code, ip))
                .isExactlyInstanceOf(UsageException.InvalidCountry.class);

        verifyNoInteractions(historyService);
    }
}