package com.tinyleaps.auth.service;

import com.tinyleaps.auth.dto.AuthDtos.*;
import com.tinyleaps.auth.entity.*;
import com.tinyleaps.auth.repository.GuestDeviceRepository;
import com.tinyleaps.auth.repository.UserRepository;
import com.tinyleaps.auth.service.SocialIdentityVerifier.SocialIdentity;
import com.tinyleaps.common.api.ApiException;
import com.tinyleaps.common.logging.TLLog;
import com.tinyleaps.common.security.JwtTokenService;
import com.tinyleaps.common.security.TokenClaims;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Guest and registered authentication flows.
 * <pre>
 * First visit      -> create guest + device link -> tokens
 * Access expired   -> refresh (rotate) -> tokens
 * Refresh invalid  -> recover guest by device id -> tokens, else new guest
 * Social sign-in   -> existing account, or upgrade this device's guest, or new registered user
 * </pre>
 */
@Service
public class AuthService {

    private final UserRepository users;
    private final GuestDeviceRepository devices;
    private final TokenService tokens;
    private final ActivityService activities;
    private final SocialIdentityVerifier socialVerifier;
    private final JwtTokenService jwt;

    public AuthService(UserRepository users, GuestDeviceRepository devices, TokenService tokens,
                       ActivityService activities, SocialIdentityVerifier socialVerifier, JwtTokenService jwt) {
        this.users = users;
        this.devices = devices;
        this.tokens = tokens;
        this.activities = activities;
        this.socialVerifier = socialVerifier;
        this.jwt = jwt;
    }

    /** Creates a guest for a new device, or recovers the guest identity already linked to it. */
    @Transactional
    public AuthSession guest(GuestAuthRequest req) {
        TLLog.process("GUEST_AUTH");
        Optional<GuestDevice> existing = devices.findByDeviceId(req.deviceId());
        if (existing.isPresent()) {
            GuestDevice device = existing.get();
            Optional<User> linked = users.findById(device.getUserId()).filter(u -> u.isGuest() && u.isActive());
            if (linked.isPresent()) {
                User guest = linked.get();
                TLLog.user(guest.getId().toString());
                device.seen();
                guest.touch();
                activities.record(guest.getId(), ActivityType.GUEST_RECOVERED, "Guest identity recovered by device", null, req.deviceId());
                return session(guest, tokens.issue(guest, req.deviceId()), "RECOVERED_GUEST");
            }
            // Device previously belonged to a registered (or disabled) account: give it a fresh guest.
            User guest = users.save(User.newGuest());
            TLLog.user(guest.getId().toString());
            device.relink(guest.getId());
            activities.record(guest.getId(), ActivityType.GUEST_CREATED, "Guest created (device relinked)", null, req.deviceId());
            return session(guest, tokens.issue(guest, req.deviceId()), "NEW_GUEST");
        }
        User guest = users.save(User.newGuest());
        TLLog.user(guest.getId().toString());
        devices.save(new GuestDevice(req.deviceId(), guest.getId(), req.platform()));
        activities.record(guest.getId(), ActivityType.GUEST_CREATED, "Guest created on first visit", null, req.deviceId());
        return session(guest, tokens.issue(guest, req.deviceId()), "NEW_GUEST");
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AuthSession refresh(RefreshRequest req) {
        TLLog.process("TOKEN_REFRESH");
        TokenService.RefreshResult result = tokens.refresh(req.refreshToken());
        return session(result.user(), result.tokens(), "REFRESHED");
    }

    @Transactional
    public AuthSession social(AuthProvider provider, SocialAuthRequest req) {
        TLLog.process(provider.name() + "_SIGN_IN");
        SocialIdentity id = socialVerifier.verify(provider, req.idToken());
        String displayName = id.name() != null ? id.name() : req.displayName();

        // 1. Returning registered user with this provider account.
        Optional<User> byProvider = users.findByAuthProviderAndProviderSubject(provider, id.subject());
        if (byProvider.isPresent()) {
            return signIn(byProvider.get(), req.deviceId(), provider);
        }
        // 2. Same verified email already registered through another provider.
        if (id.email() != null && id.emailVerified()) {
            Optional<User> byEmail = users.findByEmailIgnoreCase(id.email()).filter(u -> !u.isGuest());
            if (byEmail.isPresent()) {
                return signIn(byEmail.get(), req.deviceId(), provider);
            }
        }
        String email = id.emailVerified() ? id.email() : null;
        // 3. Upgrade the guest that is using this device, keeping their UUID and history.
        Optional<GuestDevice> device = devices.findByDeviceId(req.deviceId());
        Optional<User> guest = device.flatMap(d -> users.findById(d.getUserId())).filter(u -> u.isGuest() && u.isActive());
        if (guest.isPresent()) {
            User user = guest.get();
            TLLog.user(user.getId().toString());
            tokens.revokeAll(user.getId());
            user.upgrade(provider, id.subject(), email, displayName);
            user.touch();
            activities.record(user.getId(), ActivityType.GUEST_UPGRADED, "Guest upgraded via " + provider, null, req.deviceId());
            return session(user, tokens.issue(user, req.deviceId()), "UPGRADED_GUEST");
        }
        // 4. Brand-new registered user.
        User user = users.save(User.newRegistered(provider, id.subject(), email, displayName));
        TLLog.user(user.getId().toString());
        activities.record(user.getId(), ActivityType.SOCIAL_SIGN_UP, "Registered via " + provider, null, req.deviceId());
        return session(user, tokens.issue(user, req.deviceId()), "SIGNED_UP");
    }

    @Transactional
    public void logout(UUID userUuid, LogoutRequest req) {
        TLLog.process("LOGOUT");
        tokens.revoke(req.refreshToken(), userUuid);
        activities.record(userUuid, ActivityType.LOGOUT, "User logged out", null, null);
    }

    @Transactional(readOnly = true)
    public UserProfile profile(UUID userUuid) {
        TLLog.process("GET_PROFILE");
        User u = users.findById(userUuid).orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND"));
        return new UserProfile(u.getId().toString(), u.getAccountType().name(), u.getEmail(), u.getDisplayName(),
                u.getAuthProvider() == null ? null : u.getAuthProvider().name(), u.getCreatedAt(), u.getLastActiveAt());
    }

    /**
     * Validates credentials presented by the WebView before a game is rendered:
     * token must be valid, belong to the claimed UUID, and the user must be active.
     */
    @Transactional
    public ValidationResult validate(String bearerToken, String claimedUserUuid, String gameId) {
        TLLog.process(gameId == null ? "VALIDATE_TOKEN" : "VALIDATE_GAME_ACCESS");
        TokenClaims claims = jwt.verify(bearerToken)
                .orElseThrow(() -> ApiException.unauthorized("ACCESS_TOKEN_INVALID"));
        TLLog.user(claims.userUuid());
        if (claimedUserUuid != null && !claimedUserUuid.equals(claims.userUuid())) {
            throw ApiException.unauthorized("USER_UUID_MISMATCH");
        }
        User user = users.findById(UUID.fromString(claims.userUuid()))
                .filter(User::isActive)
                .orElseThrow(() -> ApiException.unauthorized("USER_INACTIVE"));
        user.touch();
        if (gameId != null) {
            activities.record(user.getId(), ActivityType.GAME_LAUNCH, "Game launched", "{\"gameId\":\"" + gameId + "\"}", null);
        }
        return new ValidationResult(true, user.getId().toString(), user.getAccountType().name(), gameId);
    }

    private AuthSession signIn(User user, String deviceId, AuthProvider provider) {
        if (!user.isActive()) throw ApiException.forbidden("USER_INACTIVE");
        TLLog.user(user.getId().toString());
        user.touch();
        activities.record(user.getId(), ActivityType.SOCIAL_SIGN_IN, "Signed in via " + provider, null, deviceId);
        return session(user, tokens.issue(user, deviceId), "SIGNED_IN");
    }

    private AuthSession session(User u, TokenPair t, String type) {
        return new AuthSession(u.getId().toString(), u.getAccountType().name(), u.getEmail(), u.getDisplayName(),
                "Bearer", t.accessToken(), t.accessExpiresAt(), t.refreshToken(), t.refreshExpiresAt(), type);
    }
}
