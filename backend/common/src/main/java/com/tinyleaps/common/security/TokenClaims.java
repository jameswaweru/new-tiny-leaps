package com.tinyleaps.common.security;

import java.time.Instant;

/** Validated claims of a Tiny Leaps access token. */
public record TokenClaims(String userUuid, String accountType, String tokenId, Instant expiresAt) {
}
