package com.tinyleaps.gateway.config;

import com.tinyleaps.common.security.JwtTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/** All traffic enters here and is routed to the owning micro service. */
@Configuration
public class RoutesConfig {

    @Bean
    public RouteLocator tinyLeapsRoutes(RouteLocatorBuilder builder, GatewayProperties props) {
        RouteLocatorBuilder.Builder routes = builder.routes()
                .route("auth-service", r -> r.path("/api/v1/auth/**").uri(props.authUri()))
                .route("catalogue-service", r -> r.path("/api/v1/catalogue/**").uri(props.catalogueUri()));
        // One route per standalone game service: {baseUrl}/games/{gameId}
        props.games().forEach((gameId, uri) ->
                routes.route("game-" + gameId, r -> r.path("/games/" + gameId, "/games/" + gameId + "/**").uri(uri)));
        return routes.build();
    }

    @Bean
    public JwtTokenService jwtTokenService(@Value("${tinyleaps.jwt.secret}") String secret) {
        // TTL is only used for issuing; the gateway only verifies.
        return new JwtTokenService(secret, Duration.ofMinutes(15));
    }

    @Bean
    public WebClient authWebClient(WebClient.Builder builder, GatewayProperties props) {
        return builder.baseUrl(props.authUri()).build();
    }
}
