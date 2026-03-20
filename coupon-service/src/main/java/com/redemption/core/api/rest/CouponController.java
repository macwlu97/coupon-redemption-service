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
     * The IP address is automatically retrieved from the request.
     */
    @PostMapping("/{code}/redeem")
    public ResponseEntity<Void> redeem(
            @PathVariable String code,
            HttpServletRequest request) {

        // request.getRemoteAddr() to klucz do GeoIP
        service.processRedemption(code, request.getRemoteAddr());
        return ResponseEntity.accepted().build();
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
     * Retrieves all coupons (for testing/viewing purposes).
     */
    @GetMapping
    public List<CouponResponse> getAll() {
        return service.findAll();
    }
}