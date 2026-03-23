package com.redemption.usage.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import java.time.temporal.ChronoUnit;

class UsageHistoryTest {

    @Test
    @DisplayName("Should correctly initialize UsageHistory with provided data")
    void shouldCreateUsageHistory() {
        // GIVEN
        String code = "WINTER2026";
        String userId = "user-123";

        // WHEN
        UsageHistory history = new UsageHistory(code, userId);

        // THEN
        assertThat(history.getCouponCode()).isEqualTo(code);
        assertThat(history.getUserId()).isEqualTo(userId);

        // Sprawdzamy, czy data została ustawiona na "teraz" (z tolerancją 1 sekundy)
        assertThat(history.getRedeemedAt())
                .isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS));
    }
}