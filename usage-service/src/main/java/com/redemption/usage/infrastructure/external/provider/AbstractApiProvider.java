package com.redemption.usage.infrastructure.external.provider;

import com.redemption.usage.infrastructure.external.dto.AbstractApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AbstractApiProvider implements GeoIpProvider {
    private final RestClient restClient;
    private final String apiKey;

    public AbstractApiProvider(
            RestClient.Builder builder,
            @Value("${app.geoip.abstract-api.url}") String baseUrl,
            @Value("${app.geoip.abstract-api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public String fetchCountryCode(String ip) {
        // FIX: Abstract API strictly requires a single IP. Extract the first one from the chain.
        String cleanIp = (ip != null && ip.contains(",")) ? ip.split(",")[0].trim() : ip;

        try {
            var response = restClient.get()
                    .uri(uri -> uri.path("/v1/")
                            .queryParam("api_key", apiKey)
                            .queryParam("ip_address", cleanIp) // Use cleaned IP here
                            .build())
                    .retrieve()
                    .body(AbstractApiResponse.class);

            return (response != null && response.location() != null)
                    ? response.location().countryCode()
                    : null;
        } catch (Exception e) {
            // Log error to allow the management service to handle the failover
            throw new RuntimeException("Abstract API failed for IP: " + cleanIp, e);
        }
    }

    @Override public int getPriority() { return 2; }
    @Override public boolean supports(String name) { return "abstract".equals(name); }
}