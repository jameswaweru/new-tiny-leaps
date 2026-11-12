package com.tinyleaps.gateway.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinyleaps.common.api.ApiResponse;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/** Writes gateway-generated errors: unified JSON for APIs, a friendly page for game WebViews. */
public final class Responses {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Responses() {}

    public static Mono<Void> error(ServerWebExchange exchange, int status, String customerMessage, String description) {
        ServerHttpResponse res = exchange.getResponse();
        if (res.isCommitted()) return Mono.empty();
        res.setStatusCode(HttpStatus.valueOf(status));
        boolean game = exchange.getRequest().getPath().value().startsWith("/games/");
        byte[] body;
        if (game) {
            res.getHeaders().setContentType(MediaType.TEXT_HTML);
            body = friendlyPage(customerMessage).getBytes(StandardCharsets.UTF_8);
        } else {
            res.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            try {
                body = MAPPER.writeValueAsBytes(ApiResponse.error(status, customerMessage, description));
            } catch (JsonProcessingException e) {
                body = ("{\"statusCode\":" + status + "}").getBytes(StandardCharsets.UTF_8);
            }
        }
        res.getHeaders().set("Cache-Control", "no-store");
        DataBuffer buffer = res.bufferFactory().wrap(body);
        return res.writeWith(Mono.just(buffer));
    }

    private static String friendlyPage(String message) {
        String safe = message.replace("<", "&lt;").replace(">", "&gt;");
        return """
                <!doctype html><html><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width,initial-scale=1">
                <title>Tiny Leaps</title>
                <style>body{margin:0;height:100vh;display:flex;align-items:center;justify-content:center;
                font-family:system-ui,sans-serif;background:#FFF6EC;color:#5B4B8A;text-align:center}
                .b{font-size:72px}.m{font-size:20px;padding:0 24px}</style></head>
                <body><div><div class="b">&#x1F438;</div><p class="m">%s</p></div></body></html>
                """.formatted(safe);
    }
}
