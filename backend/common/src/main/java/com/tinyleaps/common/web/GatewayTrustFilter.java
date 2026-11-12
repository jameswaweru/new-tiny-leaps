package com.tinyleaps.common.web;

import com.tinyleaps.common.api.Headers;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Rejects API calls that did not come through the gateway. Services trust the {@code X-User-UUID}
 * header only because the gateway sets it after validating the token, so direct calls must be blocked.
 */
public class GatewayTrustFilter extends OncePerRequestFilter {

    private final byte[] gatewayKey;

    public GatewayTrustFilter(String gatewayKey) {
        this.gatewayKey = gatewayKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = request.getHeader(Headers.GATEWAY_KEY);
        if (key == null || !MessageDigest.isEqual(gatewayKey, key.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(403);
            response.setContentType("application/json");
            response.getWriter().write("{\"statusCode\":403,\"customerMessage\":\"You are not allowed to do that.\","
                    + "\"description\":\"Requests must be routed through the Tiny Leaps gateway\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
