package com.tinyleaps.gateway.support;

import com.tinyleaps.common.logging.TLLog;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

/** Unified error envelope for unmatched routes and unreachable downstream services. */
@Component
@Order(-2)
public class GatewayErrorHandler implements WebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        String path = exchange.getRequest().getPath().value();
        if (ex instanceof ResponseStatusException rse) {
            int code = rse.getStatusCode().value();
            if (code == 404) {
                return Responses.error(exchange, 404, "We couldn't find what you were looking for.", "NO_ROUTE: " + path);
            }
            if (code == 503 || code == 504) {
                return Responses.error(exchange, 503, "We're having a little nap. Please try again shortly.",
                        "SERVICE_UNAVAILABLE: " + rse.getReason());
            }
            return Responses.error(exchange, code, "Something went wrong. Please try again.", rse.getReason());
        }
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        if (root instanceof ConnectException || root instanceof TimeoutException) {
            TLLog.error("Downstream unavailable for " + path, root);
            return Responses.error(exchange, 503, "We're having a little nap. Please try again shortly.",
                    "SERVICE_UNAVAILABLE");
        }
        TLLog.error("Gateway error for " + path, ex);
        return Responses.error(exchange, 500, "Oops! Something went wrong. Please try again.", "GATEWAY_ERROR");
    }
}
