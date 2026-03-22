package com.redemption.usage.infrastructure.external.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AbstractApiResponse(
        @JsonProperty("location") LocationData location
) {
    public record LocationData(
            @JsonProperty("country_code") String countryCode
    ) {}
}