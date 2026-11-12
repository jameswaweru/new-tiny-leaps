package com.tinyleaps.gateway.filter;

import com.tinyleaps.common.api.Headers;
import com.tinyleaps.common.logging.TLLog;
import org.reactivestreams.Publisher;
import org.slf4j.event.Level;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/** One log line per proxied exchange, in the Tiny Leaps format, including the JSON response body. */
@Component
public class GatewayLoggingFilter implements GlobalFilter, Ordered {

    public static final String ATTR_USER = "tl.userUuid";
    private static final int MAX_CAPTURE = 4096;

    @Override
    public int getOrder() {
        return -200;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.currentTimeMillis();
        String requestId = exchange.getRequest().getHeaders().getFirst(Headers.REQUEST_ID);
        String rid = requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId;
        ServerHttpRequest request = exchange.getRequest().mutate().headers(h -> h.set(Headers.REQUEST_ID, rid)).build();
        exchange.getResponse().getHeaders().set(Headers.REQUEST_ID, rid);

        AtomicReference<String> captured = new AtomicReference<>();
        ServerHttpResponse original = exchange.getResponse();
        ServerHttpResponseDecorator decorated = new ServerHttpResponseDecorator(original) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                MediaType type = getHeaders().getContentType();
                if (type == null || !type.getSubtype().contains("json")) {
                    return super.writeWith(body);
                }
                return DataBufferUtils.join(Flux.from(body)).flatMap(joined -> {
                    byte[] bytes = new byte[joined.readableByteCount()];
                    joined.read(bytes);
                    DataBufferUtils.release(joined);
                    captured.set(new String(bytes, 0, Math.min(bytes.length, MAX_CAPTURE), StandardCharsets.UTF_8));
                    return super.writeWith(Mono.just(bufferFactory().wrap(bytes)));
                });
            }
        };

        ServerWebExchange ex = exchange.mutate().request(request).response(decorated).build();
        return chain.filter(ex).doFinally(signal -> {
            long tat = System.currentTimeMillis() - start;
            Route route = ex.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            int status = ex.getResponse().getStatusCode() == null ? 0 : ex.getResponse().getStatusCode().value();
            String req = request.getMethod() + " " + request.getURI().getRawPath()
                    + (request.getURI().getRawQuery() != null ? "?" + request.getURI().getRawQuery() : "")
                    + " requestId=" + rid;
            String res = "status=" + status + " body=" + (captured.get() != null ? captured.get()
                    : "<" + ex.getResponse().getHeaders().getContentType() + ">");
            Level level = status >= 500 ? Level.ERROR : status >= 400 ? Level.WARN : Level.INFO;
            TLLog.logAs(ex.getAttribute(ATTR_USER), level, "ROUTE_" + (route == null ? "NONE" : route.getId()),
                    "Gateway exchange " + signal, req, res, tat);
        });
    }
}
