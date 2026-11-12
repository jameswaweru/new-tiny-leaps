package com.tinyleaps.gateway.filter;

import com.tinyleaps.common.api.Headers;
import com.tinyleaps.common.logging.TLLog;
import com.tinyleaps.common.security.JwtTokenService;
import com.tinyleaps.common.security.TokenClaims;
import com.tinyleaps.gateway.config.GatewayProperties;
import com.tinyleaps.gateway.support.Responses;
import org.slf4j.event.Level;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Central token validation.
 * <ul>
 *   <li>Public: guest/refresh/social auth endpoints and static game assets.</li>
 *   <li>Everything else needs a valid Bearer access token; the gateway then sets the trusted
 *       {@code X-User-UUID} / {@code X-Account-Type} headers for downstream services.</li>
 *   <li>Opening a game ({@code /games/{gameId}}) additionally asks auth-service to validate the
 *       credentials the WebView sent (token + UUID) before the game is rendered.</li>
 * </ul>
 */
@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private static final Set<String> PUBLIC_POST = Set.of(
            "/api/v1/auth/guest", "/api/v1/auth/refresh", "/api/v1/auth/google", "/api/v1/auth/apple");
    private static final Pattern GAME_PATH = Pattern.compile("^/games/([A-Za-z0-9_\\-]+)(/.*)?$");
    private static final Pattern GAME_ASSET = Pattern.compile("^/games/[A-Za-z0-9_\\-]+/assets/.+$");

    private final JwtTokenService jwt;
    private final GatewayProperties props;
    private final WebClient authClient;

    public AuthenticationFilter(JwtTokenService jwt, GatewayProperties props, WebClient authWebClient) {
        this.jwt = jwt;
        this.props = props;
        this.authClient = authWebClient;
    }

    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();
        String path = req.getPath().pathWithinApplication().value();

        if (isPublic(req.getMethod(), path)) {
            ServerHttpRequest forwarded = req.mutate().headers(h -> {
                h.remove(Headers.USER_UUID);
                h.remove(Headers.ACCOUNT_TYPE);
                h.set(Headers.GATEWAY_KEY, props.gatewayKey());
            }).build();
            return chain.filter(exchange.mutate().request(forwarded).build());
        }

        String token = JwtTokenService.extractBearer(req.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        Optional<TokenClaims> verified = jwt.verify(token);
        if (verified.isEmpty()) {
            TLLog.logAs(null, Level.WARN, "TOKEN_VALIDATION", "Rejected: missing/invalid/expired access token",
                    req.getMethod() + " " + path, "status=401", null);
            return Responses.error(exchange, 401, "Your session has expired. Please sign in again.", "ACCESS_TOKEN_INVALID");
        }
        TokenClaims claims = verified.get();
        exchange.getAttributes().put(GatewayLoggingFilter.ATTR_USER, claims.userUuid());

        String claimedUuid = req.getHeaders().getFirst(Headers.USER_UUID);
        if (claimedUuid != null && !claimedUuid.equals(claims.userUuid())) {
            TLLog.logAs(claims.userUuid(), Level.WARN, "TOKEN_VALIDATION", "Rejected: X-User-UUID does not match token",
                    req.getMethod() + " " + path, "status=401", null);
            return Responses.error(exchange, 401, "Your session has expired. Please sign in again.", "USER_UUID_MISMATCH");
        }

        Matcher game = GAME_PATH.matcher(path);
        boolean isGame = game.matches();
        ServerHttpRequest forwarded = req.mutate().headers(h -> {
            h.set(Headers.USER_UUID, claims.userUuid());
            h.set(Headers.ACCOUNT_TYPE, claims.accountType());
            h.set(Headers.GATEWAY_KEY, props.gatewayKey());
            if (isGame) h.remove(HttpHeaders.AUTHORIZATION); // games never need the raw token
        }).build();
        ServerWebExchange next = exchange.mutate().request(forwarded).build();

        if (!isGame) {
            return chain.filter(next);
        }
        String gameId = game.group(1);
        return validateGameAccess(token, claims.userUuid(), gameId, req.getHeaders().getFirst(Headers.REQUEST_ID))
                .flatMap(ok -> ok ? chain.filter(next)
                        : Responses.error(exchange, 401, "Please go back and tap the game again.", "GAME_ACCESS_DENIED"));
    }

    private Mono<Boolean> validateGameAccess(String token, String userUuid, String gameId, String requestId) {
        long start = System.currentTimeMillis();
        return authClient.get().uri("/api/v1/auth/validate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header(Headers.USER_UUID, userUuid)
                .header(Headers.GAME_ID, gameId)
                .header(Headers.GATEWAY_KEY, props.gatewayKey())
                .header(Headers.REQUEST_ID, requestId == null ? "" : requestId)
                .exchangeToMono(res -> res.releaseBody().thenReturn(res.statusCode().is2xxSuccessful()))
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(e -> {
                    TLLog.logAs(userUuid, Level.ERROR, "VALIDATE_GAME_ACCESS", "auth-service unreachable: " + e.getMessage(),
                            "gameId=" + gameId, null, System.currentTimeMillis() - start);
                    return Mono.just(false);
                })
                .doOnNext(ok -> TLLog.logAs(userUuid, ok ? Level.INFO : Level.WARN, "VALIDATE_GAME_ACCESS",
                        ok ? "Game access granted" : "Game access denied", "gameId=" + gameId,
                        "valid=" + ok, System.currentTimeMillis() - start));
    }

    private static boolean isPublic(HttpMethod method, String path) {
        if (HttpMethod.POST.equals(method) && PUBLIC_POST.contains(path)) return true;
        return HttpMethod.GET.equals(method) && GAME_ASSET.matcher(path).matches() && !path.contains("..");
    }
}
