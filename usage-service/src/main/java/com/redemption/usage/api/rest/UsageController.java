package com.redemption.usage.api.rest;

import com.redemption.usage.application.UsageApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usages")
@RequiredArgsConstructor
public class UsageController {

    private final UsageApplicationService usageService;

    /**
     * Entry point for users to redeem a coupon.
     * Extracts IP and delegates to the application service.
     */
    @PostMapping("/{code}/redeem")
    public ResponseEntity<Void> redeem(
            @PathVariable String code,
            HttpServletRequest request) {

        String ip = extractIp(request);
        // FIXED: Method name changed from redeemCoupon to redeem to match service
        usageService.redeem(code, ip);

        return ResponseEntity.ok().build();
    }

    private String extractIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return (ip == null || ip.isEmpty()) ? request.getRemoteAddr() : ip;
    }
}