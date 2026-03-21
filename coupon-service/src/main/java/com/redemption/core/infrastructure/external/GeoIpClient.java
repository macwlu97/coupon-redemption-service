package com.redemption.core.infrastructure.external;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class GeoIpClient {
    private final RestClient restClient;

    public GeoIpClient(RestClient.Builder builder) {
        this.restClient = builder.baseUrl("https://ipapi.co/").build();
    }

    /**
     * Fetches country code based on IP address.
     * Includes automatic retry mechanism for network-related failures.
     */
    @Retry(name = "geoip", fallbackMethod = "fallbackCountry")
    public String fetchCountryCode(String ip) {
        if (isLocal(ip)) {
            return "PL";
        }

        log.info("Requesting country code for IP: {}", ip);

        var response = restClient.get()
                .uri("{ip}/json/", ip)
                .retrieve()
                .body(GeoIpResponse.class);

        if (response == null || response.countryCode() == null) {
            throw new RestClientException("Invalid response from GeoIP provider");
        }

        return response.countryCode();
    }

    /**
     * Fallback method executed when all retry attempts fail.
     * It prevents the whole system from crashing when the external API is down.
     */
    public String fallbackCountry(String ip, Throwable t) {
        log.error("GeoIP service unavailable for IP: {}. Error: {}. Using fallback: UNKNOWN", ip, t.getMessage());
        // Business decision: In a real app, we might allow the coupon or block it.
        // For now, we return UNKNOWN which will likely fail validation in the Coupon entity.
        return "UNKNOWN";
    }

    private boolean isLocal(String ip) {
        return "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip);
    }
}