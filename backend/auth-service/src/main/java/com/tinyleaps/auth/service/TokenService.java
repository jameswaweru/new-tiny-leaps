package com.tinyleaps.auth.service;

import com.tinyleaps.auth.config.AuthProperties;
import com.tinyleaps.auth.entity.ActivityType;
import com.tinyleaps.auth.entity.RefreshToken;
import com.tinyleaps.auth.entity.User;
import com.tinyleaps.auth.repository.RefreshTokenRepository;
import com.tinyleaps.auth.repository.UserRepository;
import com.tinyleaps.common.api.ApiException;
import com.tinyleaps.common.logging.TLLog;
import com.tinyleaps.common.security.JwtTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/** Issues access/refresh token pairs and performs refresh-token rotation with reuse detection. */
@Service
public class TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JwtTokenService jwt;
    private final AuthProperties props;
    private final RefreshTokenRepository refreshTokens;
    private final UserRepository users;
    private final ActivityService activities;

    public TokenService(JwtTokenService jwt, AuthProperties props, RefreshTokenRepository refreshTokens,
                        UserRepository users, ActivityService activities) {
        this.jwt = jwt;
        this.props = props;
        this.refreshTokens = refreshTokens;
        this.users = users;
        this.activities = activities;
    }

    @Transactional
    public TokenPair issue(User user, String deviceId) {
        String access = jwt.issueAccessToken(user.getId(), user.getAccountType().name());
        Instant accessExp = Instant.now().plus(jwt.accessTtl());
        String rawRefresh = newOpaqueToken();
        Instant refreshExp = Instant.now().plus(props.jwt().refreshTtl());
        refreshTokens.save(new RefreshToken(user.getId(), hash(rawRefresh), deviceId, refreshExp));
        return new TokenPair(access, accessExp, rawRefresh, refreshExp);
    }

    /** Rotates a refresh token: the presented token is revoked and a new pair is issued. */
    @Transactional(noRollbackFor = ApiException.class)
    public RefreshResult refresh(String rawRefreshToken) {
        RefreshToken current = refreshTokens.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(() -> ApiException.unauthorized("REFRESH_TOKEN_INVALID: unknown refresh token"));
        TLLog.user(current.getUserId().toString());

        if (current.isRevoked()) {
            // A revoked token being replayed means it may have been stolen: kill every session of this user.
            int revoked = refreshTokens.revokeAllForUser(current.getUserId());
            activities.recordInNewTx(current.getUserId(), ActivityType.REFRESH_TOKEN_REUSE_DETECTED,
                    "Revoked refresh token replayed; " + revoked + " active token(s) revoked", null, current.getDeviceId());
            throw ApiException.unauthorized("REFRESH_TOKEN_REVOKED: token reuse detected");
        }
        if (current.isExpired()) {
            throw ApiException.unauthorized("REFRESH_TOKEN_EXPIRED");
        }
        User user = users.findById(current.getUserId())
                .filter(User::isActive)
                .orElseThrow(() -> ApiException.unauthorized("USER_INACTIVE"));

        TokenPair pair = issue(user, current.getDeviceId());
        RefreshToken next = refreshTokens.findByTokenHash(hash(pair.refreshToken())).orElseThrow();
        current.revoke(next.getId());
        user.touch();
        activities.record(user.getId(), ActivityType.TOKEN_REFRESHED, "Access token refreshed", null, current.getDeviceId());
        return new RefreshResult(user, pair);
    }

    @Transactional
    public void revoke(String rawRefreshToken, UUID userUuid) {
        refreshTokens.findByTokenHash(hash(rawRefreshToken))
                .filter(t -> t.getUserId().equals(userUuid))
                .ifPresent(t -> t.revoke(null));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void revokeAll(UUID userUuid) {
        refreshTokens.revokeAllForUser(userUuid);
    }

    public record RefreshResult(User user, TokenPair tokens) {}

    private static String newOpaqueToken() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
