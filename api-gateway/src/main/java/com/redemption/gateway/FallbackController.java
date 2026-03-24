package com.redemption.gateway;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/coupon-service")
    public Map<String, String> couponServiceFallback() {
        return Collections.singletonMap("message",
                "Coupon Service is taking too long to respond or is down. Please try again later.");
    }

    @RequestMapping("/usage-service")
    public Map<String, String> usageServiceFallback() {
        return Collections.singletonMap("message",
                "Redemption service is currently unavailable. Your request could not be processed.");
    }
}