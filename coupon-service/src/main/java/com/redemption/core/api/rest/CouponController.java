package com.redemption.core.api.rest;

import com.redemption.core.api.rest.dto.CreateCouponRequest;
import com.redemption.core.api.rest.dto.CouponResponse;
import com.redemption.core.application.CouponApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponApplicationService service;

    /**
     * Creates a new coupon.
     * Added explicit @Valid to trigger Spring Validation on the record.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CouponResponse create(@RequestBody @Valid CreateCouponRequest request) {
        return service.createCoupon(request);
    }

    /**
     * Retrieves coupons with pagination.
     * Returning a List is dangerous for scalability. Pageable is the senior way.
     */
    @GetMapping
    public Page<CouponResponse> getAll(@PageableDefault(size = 20) Pageable pageable) {
        return service.findAll(pageable);
    }
}