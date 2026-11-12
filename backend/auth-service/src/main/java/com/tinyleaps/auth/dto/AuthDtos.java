package com.tinyleaps.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** Request/response payloads of the auth API. */
public final class AuthDtos {
    private AuthDtos() {}

    private static final String DEVICE_ID_REGEX = "^[A-Za-z0-9._:\\-]{8,128}$";

    public record GuestAuthRequest(
            @NotBlank @Pattern(regexp = DEVICE_ID_REGEX, message = "must be 8-128 chars [A-Za-z0-9._:-]") String deviceId,
            @Size(max = 20) String platform) {}

    public record RefreshRequest(@NotBlank @Size(max = 512) String refreshToken) {}

    public record SocialAuthRequest(
            @NotBlank @Size(max = 8192) String idToken,
            @NotBlank @Pattern(regexp = DEVICE_ID_REGEX, message = "must be 8-128 chars [A-Za-z0-9._:-]") String deviceId,
            @Size(max = 20) String platform,
            @Size(max = 120) String displayName) {}

    public record LogoutRequest(@NotBlank @Size(max = 512) String refreshToken) {}

    public record AuthSession(
            String userUuid,
            String accountType,
            String email,
            String displayName,
            String tokenType,
            String accessToken,
            Instant accessTokenExpiresAt,
            String refreshToken,
            Instant refreshTokenExpiresAt,
            /** NEW_GUEST, RECOVERED_GUEST, UPGRADED_GUEST, SIGNED_IN, SIGNED_UP, REFRESHED */
            String sessionType) {}

    public record ValidationResult(boolean valid, String userUuid, String accountType, String gameId) {}

    public record UserProfile(String userUuid, String accountType, String email, String displayName,
                              String authProvider, Instant createdAt, Instant lastActiveAt) {}

    public record ActivityView(String id, String activityType, String description, String metadata, Instant createdAt) {}
}
