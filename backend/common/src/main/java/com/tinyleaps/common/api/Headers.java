package com.tinyleaps.common.api;

/** Header names shared between the gateway, services and the mobile app. */
public final class Headers {
    private Headers() {}

    /** User UUID. Sent by the app; overwritten by the gateway with the validated token subject. */
    public static final String USER_UUID = "X-User-UUID";
    /** Account type (GUEST / REGISTERED) of the validated token, set by the gateway. */
    public static final String ACCOUNT_TYPE = "X-Account-Type";
    /** Correlation id propagated across services. */
    public static final String REQUEST_ID = "X-Request-Id";
    /** Game being launched, sent with game-access validation. */
    public static final String GAME_ID = "X-Game-Id";
    /** Shared secret proving a call came through the gateway. */
    public static final String GATEWAY_KEY = "X-Gateway-Key";
}
