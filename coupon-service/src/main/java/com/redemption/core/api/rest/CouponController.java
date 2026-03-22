package com.redemption.core.api.rest;

import com.redemption.core.api.rest.dto.CreateCouponRequest;
import com.redemption.core.api.rest.dto.CouponResponse;
import com.redemption.core.application.CouponApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupon Admin API", description = "Endpoints for managing coupon definitions and limits")
public class CouponController {

    private final CouponApplicationService service;

    /**
     * Creates a new coupon.
     * Added explicit @Valid to trigger Spring Validation on the record.
     */
    @Operation(summary = "Create a new coupon", description = "Defines a new coupon with a specific usage limit and target country code.")
    @ApiResponse(responseCode = "201", description = "Coupon created successfully")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CouponResponse create(@RequestBody @Valid CreateCouponRequest request) {
        return service.createCoupon(request);
    }

    /**
     * Retrieves coupons with pagination.
     * Returning a List is dangerous for scalability. Pageable is the senior way.
     */
    @Operation(summary = "Get all coupons", description = "Retrieves a paginated list of all defined coupons.")
    @GetMapping
    public Page<CouponResponse> getAll(@PageableDefault(size = 20) Pageable pageable) {
        return service.findAll(pageable);
    }
}