package com.tinyleaps.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "tinyleaps")
public record AuthProperties(Jwt jwt, Social social) {

    public record Jwt(String secret, Duration accessTtl, Duration refreshTtl) {}

    public record Social(Provider google, Provider apple) {}

    public record Provider(List<String> clientIds) {
        public List<String> clientIdsOrEmpty() {
            return clientIds == null ? List.of() : clientIds.stream().filter(s -> s != null && !s.isBlank()).toList();
        }
    }
}
