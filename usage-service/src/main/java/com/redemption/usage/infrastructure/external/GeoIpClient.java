package com.redemption.usage.infrastructure.external;

import com.redemption.usage.infrastructure.external.dto.GeoIpResponse;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.InetSocketAddress;
import java.net.Proxy;

@Slf4j
@Component
public class GeoIpClient {
    private final RestClient restClient;
    private static final String GEO_IP_URL = "https://ipapi.co/"; //yml

    public GeoIpClient(RestClient.Builder builder) {
        // 1. Definicja serwera proxy (wybierz jeden z tabeli powyżej)
        String proxyHost = "1.231.81.166";
        int proxyPort = 3128;

        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));

        // 2. Konfiguracja fabryki żądań z obsługą proxy
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setProxy(proxy);
        factory.setConnectTimeout(5000); // Darmowe proxy bywają wolne

        this.restClient = builder.requestFactory(factory).baseUrl(GEO_IP_URL).build();
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