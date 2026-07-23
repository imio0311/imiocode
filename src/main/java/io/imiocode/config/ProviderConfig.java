package io.imiocode.config;

import com.fasterxml.jackson.annotation.JsonProperty;

record ProviderConfig(
        @JsonProperty("api-key") String apiKey,
        @JsonProperty("base-url") String baseUrl) {

    static ProviderConfig empty() {
        return new ProviderConfig(null, null);
    }

    @Override
    public String toString() {
        return "ProviderConfig[apiKey=***, baseUrl=" + baseUrl + "]";
    }
}
