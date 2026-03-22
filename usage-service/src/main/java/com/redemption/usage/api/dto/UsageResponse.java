package com.redemption.usage.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Response details after a successful coupon redemption")
public record UsageResponse(
        @Schema(description = "The coupon code that was redeemed", example = "SUMMER2026")
        String code,

        @Schema(description = "The country detected from user IP", example = "PL")
        String detectedCountry,

        @Schema(description = "Timestamp of the redemption")
        LocalDateTime redeemedAt
) {}
