package com.redemption.core.api.rest;

import com.redemption.core.api.rest.dto.CreateCouponRequest;
import com.redemption.core.api.rest.dto.CouponResponse;
import com.redemption.core.application.CouponApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponApplicationService service;

    /**
     * Registers coupon usage.
     * It tries to extract the IP from 'X-Forwarded-For' header first to support proxies and integration tests.
     */
    @PostMapping("/{code}/redeem")
    public ResponseEntity<Void> redeem(
            @PathVariable String code,
            HttpServletRequest request) {

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }

        service.processRedemption(code, ip);
        return ResponseEntity.ok().build(); // Standard 200 OK for successful redemption
    }

    /**
     * Creates a new coupon.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CouponResponse create(@RequestBody @Valid CreateCouponRequest request) {
        return service.createCoupon(request);
    }

    /**
     * Retrieves all coupons for overview.
     */
    @GetMapping
    public List<CouponResponse> getAll() {
        return service.findAll();
    }
}