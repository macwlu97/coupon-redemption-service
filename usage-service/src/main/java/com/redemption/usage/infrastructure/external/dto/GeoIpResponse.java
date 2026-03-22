package com.redemption.usage.infrastructure.external.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeoIpResponse(
        String ip,
        String city,
        String region,

        @JsonProperty("country_code")
        String countryCode,

        @JsonProperty("country_name")
        String country
) {}