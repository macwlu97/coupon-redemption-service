package com.redemption.core.api.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating a new coupon.
 * Using Java 21 Record for immutability and boilerplate reduction.
 */
@Schema(description = "Request payload for creating a new coupon definition")
public record CreateCouponRequest(
        @Schema(description = "Desired coupon code (alphanumeric)", example = "SUMMER2026", minLength = 3, maxLength = 20)
        @NotBlank @Size(min = 3, max = 20) String code,

        @Schema(description = "Usage limit for this coupon", example = "100", minimum = "1")
        @Min(1) int usageLimit,

        @Schema(description = "Target country ISO code", example = "PL")
        @NotBlank @Size(min = 2, max = 2) String targetCountry
) {}