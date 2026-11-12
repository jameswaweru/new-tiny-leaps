package com.tinyleaps.common.web;

import com.tinyleaps.common.api.Headers;
import com.tinyleaps.common.logging.TLLog;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Logs one line per HTTP exchange with request, response and TAT in the Tiny Leaps format. */
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String requestId = request.getHeader(Headers.REQUEST_ID);
        if (requestId == null || requestId.isBlank()) requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);
        response.setHeader(Headers.REQUEST_ID, requestId);
        TLLog.user(request.getHeader(Headers.USER_UUID));

        ContentCachingRequestWrapper req = new ContentCachingRequestWrapper(request, 16 * 1024);
        ContentCachingResponseWrapper res = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(req, res);
        } finally {
            long tat = System.currentTimeMillis() - start;
            String process = TLLog.currentProcess() != null ? TLLog.currentProcess()
                    : request.getMethod() + " " + request.getRequestURI();
            String reqLog = request.getMethod() + " " + request.getRequestURI()
                    + (request.getQueryString() != null ? "?" + request.getQueryString() : "")
                    + " body=" + body(req.getContentAsByteArray(), req.getContentType());
            String resLog = "status=" + res.getStatus() + " body=" + body(res.getContentAsByteArray(), res.getContentType());
            Level level = res.getStatus() >= 500 ? Level.ERROR : res.getStatus() >= 400 ? Level.WARN : Level.INFO;
            TLLog.log(level, process, "HTTP exchange completed", reqLog, resLog, tat);
            res.copyBodyToResponse();
            TLLog.clear();
            MDC.remove("requestId");
        }
    }

    private static String body(byte[] content, String contentType) {
        if (content == null || content.length == 0) return "{}";
        if (contentType == null || !contentType.contains("json")) {
            return "<" + (contentType == null ? "binary" : contentType) + " " + content.length + " bytes>";
        }
        return new String(content, StandardCharsets.UTF_8);
    }
}
