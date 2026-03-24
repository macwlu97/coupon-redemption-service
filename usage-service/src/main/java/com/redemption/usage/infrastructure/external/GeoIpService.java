package com.redemption.usage.infrastructure.external;

import com.redemption.usage.domain.exception.GeoIpConfigurationException;
import com.redemption.usage.infrastructure.external.provider.GeoIpProvider;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Orchestrates GeoIP lookups using multiple providers to ensure high availability.
 * Implements Strategy and Fallback patterns to handle 3rd party API failures.
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class GeoIpService {

    private final List<GeoIpProvider> providers;

    @PostConstruct
    public void validateConfig() {
        if (providers.isEmpty()) {
            throw new GeoIpConfigurationException("No GeoIP providers found! Application cannot function.");
        }
    }

    @CircuitBreaker(name = "geoipCB", fallbackMethod = "fallbackForCircuitBreaker")
    @Retry(name = "geoip")
    public String getCountryCode(String rawIp) {
        // 1. CLEAN IP IMMEDIATELY
        String cleanIp = resolveCleanIp(rawIp);
        log.info("Processing GeoIP lookup for cleaned IP: {}", cleanIp);

        if (isLocal(cleanIp)) return "PL";

        // 2. Use the cleaned IP for the primary provider
        return providers.stream()
                .filter(p -> p.getPriority() == 1)
                .findFirst()
                .map(p -> p.fetchCountryCode(cleanIp)) // PASS CLEAN IP
                .orElseThrow(() -> new GeoIpConfigurationException("Primary GeoIP provider (priority 1) not found."));
    }

    public String fallbackForCircuitBreaker(String rawIp, Throwable t) {
        // Even in fallback, we must ensure the IP is clean
        String cleanIp = resolveCleanIp(rawIp);
        log.error("Primary GeoIP provider failed ({}). Switching to secondary for IP: {}", t.getMessage(), cleanIp);

        return providers.stream()
                .filter(p -> p.getPriority() > 1)
                .sorted(Comparator.comparingInt(GeoIpProvider::getPriority))
                .map(provider -> tryFetchCountryCode(provider, cleanIp)) // PASS CLEAN IP
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("UNKNOWN");
    }

    /**
     * Extracts the first IP from a potential X-Forwarded-For chain.
     * Essential for Docker/Gateway environments where multiple hops occur.
     */
    private String resolveCleanIp(String ip) {
        if (ip == null || ip.isBlank()) return "127.0.0.1";
        // Take the first IP before the comma
        return ip.split(",")[0].trim();
    }

    private String tryFetchCountryCode(GeoIpProvider provider, String ip) {
        try {
            return provider.fetchCountryCode(ip);
        } catch (Exception e) {
            log.warn("Secondary provider (priority {}) failed: {}", provider.getPriority(), e.getMessage());
            return null;
        }
    }

    private boolean isLocal(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.isLoopbackAddress();
        } catch (UnknownHostException e) {
            // This should no longer happen frequently thanks to resolveCleanIp()
            log.warn("Could not resolve host: {}", ip);
            return false;
        }
    }
}