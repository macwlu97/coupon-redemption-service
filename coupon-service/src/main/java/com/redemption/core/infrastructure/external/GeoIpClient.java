package com.redemption.core.infrastructure.external;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.Objects;

@Component
public class GeoIpClient {
    private final RestClient restClient;

    public GeoIpClient(RestClient.Builder builder) {
        this.restClient = builder.baseUrl("https://ipapi.co/").build();
    }

    public String fetchCountryCode(String ip) {
        if (ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1")) return "PL";

        var response = restClient.get()
                .uri("{ip}/json/", ip)
                .retrieve()
                .body(GeoIpResponse.class);

        return Objects.requireNonNullElse(response, new GeoIpResponse("UNKNOWN")).countryCode();
    }
}