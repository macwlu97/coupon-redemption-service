package com.redemption.usage.infrastructure.external.provider;

import com.redemption.usage.infrastructure.external.dto.GeoIpResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.net.InetSocketAddress;
import java.net.Proxy;

@Component
public class IpApiProvider implements GeoIpProvider {
    private final RestClient restClient;

    public IpApiProvider(
            RestClient.Builder builder,
            @Value("${app.geoip.ip-api.url}") String baseUrl,
            @Value("${app.geoip.proxy.host}") String proxyHost,
            @Value("${app.geoip.proxy.port}") int proxyPort,
            @Value("${app.geoip.proxy.enabled}") boolean proxyEnabled) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        if (proxyEnabled) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            factory.setProxy(proxy);
        }

        factory.setConnectTimeout(5000);

        this.restClient = builder
                .requestFactory(factory)
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public String fetchCountryCode(String ip) {
        // FIX: Extract only the first IP if X-Forwarded-For contains a chain
        String cleanIp = (ip != null && ip.contains(",")) ? ip.split(",")[0].trim() : ip;

        try {
            var response = restClient.get()
                    .uri("/{ip}/json/", cleanIp) // Now sending only '89.64.1.1'
                    .retrieve()
                    .body(GeoIpResponse.class);
            return (response != null) ? response.countryCode() : null;
        } catch (Exception e) {
            // Log the error so the circuit breaker can track it
            throw new RuntimeException("GeoIP fetch failed for IP: " + cleanIp, e);
        }
    }

    @Override public int getPriority() { return 1; }
    @Override public boolean supports(String name) { return "ipapi".equals(name); }
}