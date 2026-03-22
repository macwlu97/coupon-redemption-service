package com.redemption.usage.infrastructure.external.provider;

public interface GeoIpProvider {
    String fetchCountryCode(String ip);
    boolean supports(String providerName);
    int getPriority();
}