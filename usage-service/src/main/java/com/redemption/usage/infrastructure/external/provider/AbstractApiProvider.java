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
        var response = restClient.get()
                .uri(uri -> uri.path("/v1/")
                        .queryParam("api_key", apiKey)
                        .queryParam("ip_address", ip).build())
                .retrieve()
                .body(AbstractApiResponse.class);

        return (response != null && response.location() != null) ? response.location().countryCode() : null;
    }

    @Override public int getPriority() { return 2; }
    @Override public boolean supports(String name) { return "abstract".equals(name); }
}