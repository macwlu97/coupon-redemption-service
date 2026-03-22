package com.redemption.usage.api.rest;

import com.redemption.usage.api.dto.UsageResponse;
import com.redemption.usage.application.UsageApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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

import java.time.LocalDateTime;

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
            description = "Validates coupon code, detects user country via IP (GeoIP), and increments usage count in the coupon service."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Coupon successfully redeemed",
                    content = @Content(schema = @Schema(implementation = UsageResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid coupon, expired, or country mismatch"),
            @ApiResponse(responseCode = "409", description = "Conflict: Coupon already redeemed by this user/IP"),
            @ApiResponse(responseCode = "503", description = "Service Unavailable: Circuit Breaker is open or GeoIP provider is down")
    })
    @PostMapping("/{code}/redeem")
    public ResponseEntity<UsageResponse> redeem(
            @Parameter(description = "The unique alphanumeric coupon code", example = "SUMMER2026")
            @PathVariable String code,
            HttpServletRequest request) {

        String ip = extractIp(request);
        // FIXED: Method name changed from redeemCoupon to redeem to match service
        usageService.redeem(code, ip);

        String detectedCountry = usageService.redeem(code, ip);

        return ResponseEntity.ok(new UsageResponse(
                code.toUpperCase(),
                detectedCountry,
                LocalDateTime.now()
        ));
    }

    private String extractIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return (ip == null || ip.isEmpty()) ? request.getRemoteAddr() : ip;
    }
}