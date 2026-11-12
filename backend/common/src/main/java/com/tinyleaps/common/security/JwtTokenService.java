package com.tinyleaps.common.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Issues and verifies HS256 access tokens. The auth-service issues; the gateway (and auth-service) verify.
 * Both must share the same secret (property {@code tinyleaps.jwt.secret}, at least 32 bytes).
 */
public class JwtTokenService {

    public static final String ISSUER = "tinyleaps-auth";
    private static final String CLAIM_ACCOUNT_TYPE = "acct";
    private static final String CLAIM_TOKEN_USE = "use";

    private final byte[] secret;
    private final Duration accessTtl;

    public JwtTokenService(String secret, Duration accessTtl) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("tinyleaps.jwt.secret must be at least 32 bytes");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.accessTtl = accessTtl;
    }

    public Duration accessTtl() { return accessTtl; }

    public String issueAccessToken(UUID userUuid, String accountType) {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .subject(userUuid.toString())
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(accessTtl)))
                .claim(CLAIM_ACCOUNT_TYPE, accountType)
                .claim(CLAIM_TOKEN_USE, "access")
                .build();
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secret));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Unable to sign token", e);
        }
    }

    /** Returns claims when the token is well-formed, correctly signed, from our issuer and not expired. */
    public Optional<TokenClaims> verify(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm())) return Optional.empty();
            if (!jwt.verify(new MACVerifier(secret))) return Optional.empty();
            JWTClaimsSet c = jwt.getJWTClaimsSet();
            if (!ISSUER.equals(c.getIssuer())) return Optional.empty();
            if (!"access".equals(c.getStringClaim(CLAIM_TOKEN_USE))) return Optional.empty();
            Date exp = c.getExpirationTime();
            if (exp == null || exp.toInstant().isBefore(Instant.now())) return Optional.empty();
            return Optional.of(new TokenClaims(c.getSubject(), c.getStringClaim(CLAIM_ACCOUNT_TYPE),
                    c.getJWTID(), exp.toInstant()));
        } catch (ParseException | JOSEException e) {
            return Optional.empty();
        }
    }

    public static String extractBearer(String authorizationHeader) {
        if (authorizationHeader == null) return null;
        String h = authorizationHeader.trim();
        return h.regionMatches(true, 0, "Bearer ", 0, 7) ? h.substring(7).trim() : null;
    }
}
