package com.redemption.core.api.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCouponRequest(
        @NotBlank @Size(min = 3, max = 20) String code,
        @Min(1) int usageLimit,
        @NotBlank @Size(min = 2, max = 2) String targetCountry // np. "PL"
) {}