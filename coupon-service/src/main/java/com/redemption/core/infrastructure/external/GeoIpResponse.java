package com.redemption.core.infrastructure.external;

import com.fasterxml.jackson.annotation.JsonProperty;

record GeoIpResponse(@JsonProperty("country_code") String countryCode) {}