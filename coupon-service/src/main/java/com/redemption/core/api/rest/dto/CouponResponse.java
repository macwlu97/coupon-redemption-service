package com.redemption.core.api.rest.dto;

import java.time.LocalDateTime;

public record CouponResponse(
        String code,
        int usageLimit,
        int currentUsage,
        String targetCountry,
        LocalDateTime createdAt
) {}
