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

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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
    public String getCountryCode(String ip) {
        if (isLocal(ip)) return "PL";

        return providers.stream()
                .filter(p -> p.getPriority() == 1)
                .findFirst()
                .map(p -> p.fetchCountryCode(ip))
                .orElseThrow(() -> new GeoIpConfigurationException("Primary GeoIP provider (priority 1) not found in the system. Check your configuration."));
    }

    /**
     * Fallback method – if IpApiProvider fails (e.g., proxy error),
     * Resilience4j calls this to check AbstractApi.
     */
    public String fallbackForCircuitBreaker(String ip, Throwable t) {
        log.error("Primary GeoIP provider failed ({}). Switching to secondary...", t.getMessage());

        return providers.stream()
                .filter(p -> p.getPriority() > 1)
                .sorted(Comparator.comparingInt(GeoIpProvider::getPriority))
                .map(p -> p.fetchCountryCode(ip))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("UNKNOWN");
    }

    private boolean isLocal(String ip) {
        return "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip);
    }
}