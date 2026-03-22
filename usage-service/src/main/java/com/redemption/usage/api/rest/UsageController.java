package com.redemption.usage.api.rest;

import com.redemption.usage.application.UsageApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Usage API", description = "Operations related to coupon redemption and tracking")
public class UsageController {

    private final UsageApplicationService usageService;

    /**
     * Entry point for users to redeem a coupon.
     * Extracts IP and delegates to the application service.
     */
    @Operation(
            summary = "Redeem a coupon",
            description = "Validates coupon code, checks user IP location, and increments usage count."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coupon successfully redeemed"),
            @ApiResponse(responseCode = "400", description = "Invalid coupon or country mismatch"),
            @ApiResponse(responseCode = "409", description = "Coupon already redeemed by this user"),
            @ApiResponse(responseCode = "503", description = "System busy / Circuit Breaker open")
    })
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