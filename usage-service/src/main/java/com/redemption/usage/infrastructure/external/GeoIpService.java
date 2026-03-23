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


    /**
     * Ensures that the application has at least one GeoIP provider configured on startup.
     * Throws an exception early (Fail-Fast) if the configuration is missing.
     */
    @PostConstruct
    public void validateConfig() {
        if (providers.isEmpty()) {
            throw new GeoIpConfigurationException("No GeoIP providers found! Application cannot function.");
        }
    }

    /**
     * Attempts to resolve country code for a given IP address.
     * Uses the primary provider (priority 1) with automatic retries and circuit breaking.
     *
     * @param ip The IPv4 or IPv6 address to locate.
     * @return ISO country code (e.g., "PL").
     */
    @CircuitBreaker(name = "geoipCB", fallbackMethod = "fallbackForCircuitBreaker")
    @Retry(name = "geoip")
    public String getCountryCode(String ip) {
        if (isLocal(ip)) return "PL";

        return providers.stream()
                .filter(p -> p.getPriority() == 1)
                .findFirst()
                .map(p -> p.fetchCountryCode(ip))
                .orElseThrow(() -> new GeoIpConfigurationException("Primary GeoIP provider (priority 1) not found in the system. Check your configuration."));
    }

    /**
     * Fallback logic triggered when the primary provider fails or the Circuit Breaker is OPEN.
     * Iterates through secondary providers sorted by priority.
     *
     * @param ip The IP address that failed primary lookup.
     * @param t The exception that triggered the fallback.
     * @return Country code from a secondary provider or "UNKNOWN" as a last resort.
     */
    public String fallbackForCircuitBreaker(String ip, Throwable t) {
        log.error("Primary GeoIP provider failed ({}). Switching to secondary...", t.getMessage());

        return providers.stream()
                .filter(p -> p.getPriority() > 1)
                .sorted(Comparator.comparingInt(GeoIpProvider::getPriority))
                .map(provider -> tryFetchCountryCode(provider, ip)) // Clean and descriptive
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("UNKNOWN");
    }

    /**
     * Helper method to wrap provider calls and handle exceptions locally.
     * This keeps the stream logic clean and readable.
     */
    private String tryFetchCountryCode(GeoIpProvider provider, String ip) {
        try {
            return provider.fetchCountryCode(ip);
        } catch (Exception e) {
            log.warn("Secondary provider (priority {}) failed: {}", provider.getPriority(), e.getMessage());
            return null;
        }
    }

    /**
     * Checks if the provided IP address is a loopback address.
     * This implementation handles both IPv4 (127.0.0.1) and all IPv6 variants (e.g., ::1).
     */
    private boolean isLocal(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.isLoopbackAddress();
        } catch (UnknownHostException e) {
            log.warn("Invalid IP format detected: {}", ip);
            return false;
        }
    }
}