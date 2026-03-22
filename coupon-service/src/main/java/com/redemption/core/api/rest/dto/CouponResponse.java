package com.redemption.core.api.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Public representation of a coupon.
 */
@Schema(description = "Detailed information about a coupon status")
public record CouponResponse(
        @Schema(description = "Unique coupon identification code", example = "SUMMER2026")
        String code,

        @Schema(description = "Maximum number of allowed redemptions", example = "100")
        int usageLimit,

        @Schema(description = "Number of times this coupon has been successfully used", example = "15")
        int currentUsage,

        @Schema(description = "ISO country code where the coupon is valid", example = "PL")
        String targetCountry,

        @Schema(description = "Timestamp when the coupon was created")
        LocalDateTime createdAt
) {}