package com.tinyleaps.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "tinyleaps.gateway")
public record GatewayProperties(String gatewayKey, String authUri, String catalogueUri, Map<String, String> games) {
    public GatewayProperties {
        games = games == null ? Map.of() : Map.copyOf(games);
    }
}
