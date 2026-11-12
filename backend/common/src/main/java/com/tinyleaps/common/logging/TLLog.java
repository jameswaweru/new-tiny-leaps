package com.tinyleaps.common.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Structured logger. Every line follows the Tiny Leaps format (see logback-spring.xml):
 * <pre>time stamp= | Severity= | UserUUID= | Micro Service Name= | ProcessName= | LogMessage= | Request= | Response= | TAT= |</pre>
 * Values are carried in MDC so the layout is identical for application and framework logs.
 */
public final class TLLog {

    public static final String MDC_USER = "userUuid";
    public static final String MDC_PROCESS = "processName";
    public static final String MDC_REQUEST = "request";
    public static final String MDC_RESPONSE = "response";
    public static final String MDC_TAT = "tat";

    private static final Logger LOG = LoggerFactory.getLogger("TinyLeaps");
    private static final int MAX_FIELD = 2000;
    private static final Pattern SECRET_FIELDS = Pattern.compile(
            "(\"(?:accessToken|refreshToken|idToken|identityToken|password|token)\"\\s*:\\s*\")[^\"]*(\")",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern BEARER = Pattern.compile("(?i)(bearer\\s+)[A-Za-z0-9._\\-]+");

    private TLLog() {}

    /** Sets the user for subsequent log lines on this thread/request. */
    public static void user(String userUuid) {
        if (userUuid != null && !userUuid.isBlank()) MDC.put(MDC_USER, userUuid);
    }

    /** Sets the business process name for subsequent log lines on this thread/request. */
    public static void process(String processName) {
        if (processName != null && !processName.isBlank()) MDC.put(MDC_PROCESS, processName);
    }

    public static String currentProcess() {
        return MDC.get(MDC_PROCESS);
    }

    public static void info(String message) { log(Level.INFO, null, message, null, null, null); }
    public static void warn(String message) { log(Level.WARN, null, message, null, null, null); }
    public static void error(String message, Throwable t) {
        log(Level.ERROR, null, message + (t == null ? "" : " :: " + t.getClass().getSimpleName() + ": " + t.getMessage()),
                null, null, null);
    }

    public static void info(String process, String message) { log(Level.INFO, process, message, null, null, null); }
    public static void warn(String process, String message) { log(Level.WARN, process, message, null, null, null); }

    /** Full log line with request/response payloads and turn-around time (ms). */
    public static void log(Level level, String process, String message, String request, String response, Long tatMs) {
        Map<String, String> previous = new HashMap<>();
        put(previous, MDC_PROCESS, process);
        put(previous, MDC_REQUEST, sanitize(request));
        put(previous, MDC_RESPONSE, sanitize(response));
        put(previous, MDC_TAT, tatMs == null ? null : tatMs + "ms");
        try {
            String msg = oneLine(message);
            switch (level) {
                case ERROR -> LOG.error(msg);
                case WARN -> LOG.warn(msg);
                case DEBUG -> LOG.debug(msg);
                case TRACE -> LOG.trace(msg);
                default -> LOG.info(msg);
            }
        } finally {
            previous.forEach((k, v) -> { if (v == null) MDC.remove(k); else MDC.put(k, v); });
        }
    }

    /** Runs a log statement with an explicit user, for reactive code where MDC is not request-scoped. */
    public static void logAs(String userUuid, Level level, String process, String message,
                             String request, String response, Long tatMs) {
        String prev = MDC.get(MDC_USER);
        if (userUuid != null) MDC.put(MDC_USER, userUuid); else MDC.remove(MDC_USER);
        try {
            log(level, process, message, request, response, tatMs);
        } finally {
            if (prev == null) MDC.remove(MDC_USER); else MDC.put(MDC_USER, prev);
        }
    }

    public static void clear() {
        MDC.remove(MDC_USER);
        MDC.remove(MDC_PROCESS);
        MDC.remove(MDC_REQUEST);
        MDC.remove(MDC_RESPONSE);
        MDC.remove(MDC_TAT);
    }

    /** Masks tokens/passwords, flattens to one line and truncates. */
    public static String sanitize(String value) {
        if (value == null || value.isEmpty()) return value;
        String v = SECRET_FIELDS.matcher(value).replaceAll("$1***$2");
        v = BEARER.matcher(v).replaceAll("$1***");
        v = oneLine(v);
        return v.length() > MAX_FIELD ? v.substring(0, MAX_FIELD) + "...(truncated)" : v;
    }

    private static String oneLine(String v) {
        return v == null ? null : v.replaceAll("[\\r\\n\\t]+", " ");
    }

    private static void put(Map<String, String> previous, String key, String value) {
        if (value == null) return;
        previous.put(key, MDC.get(key));
        MDC.put(key, value);
    }
}
