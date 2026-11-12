package com.tinyleaps.auth.controller;

import com.tinyleaps.auth.dto.AuthDtos.*;
import com.tinyleaps.auth.entity.AuthProvider;
import com.tinyleaps.auth.entity.UserActivity;
import com.tinyleaps.auth.service.ActivityService;
import com.tinyleaps.auth.service.AuthService;
import com.tinyleaps.common.api.ApiException;
import com.tinyleaps.common.api.ApiResponse;
import com.tinyleaps.common.api.Headers;
import com.tinyleaps.common.api.Pagination;
import com.tinyleaps.common.logging.TLLog;
import com.tinyleaps.common.security.JwtTokenService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService auth;
    private final ActivityService activities;

    public AuthController(AuthService auth, ActivityService activities) {
        this.auth = auth;
        this.activities = activities;
    }

    /** First visit / skip registration / guest recovery. Public. */
    @PostMapping("/guest")
    public ApiResponse<AuthSession> guest(@Valid @RequestBody GuestAuthRequest req) {
        AuthSession s = auth.guest(req);
        boolean recovered = "RECOVERED_GUEST".equals(s.sessionType());
        return ApiResponse.ok(recovered ? "Welcome back!" : "Welcome to Tiny Leaps!",
                recovered ? "Guest identity recovered" : "Guest user created", s);
    }

    /** Rotates the refresh token and issues a new access token. Public. */
    @PostMapping("/refresh")
    public ApiResponse<AuthSession> refresh(@Valid @RequestBody RefreshRequest req) {
        return ApiResponse.ok("Session refreshed", "New token pair issued", auth.refresh(req));
    }

    @PostMapping("/google")
    public ApiResponse<AuthSession> google(@Valid @RequestBody SocialAuthRequest req) {
        return socialResponse(auth.social(AuthProvider.GOOGLE, req));
    }

    @PostMapping("/apple")
    public ApiResponse<AuthSession> apple(@Valid @RequestBody SocialAuthRequest req) {
        return socialResponse(auth.social(AuthProvider.APPLE, req));
    }

    @PostMapping("/logout")
    public ApiResponse<Object> logout(@RequestHeader(Headers.USER_UUID) String userUuid,
                                      @Valid @RequestBody LogoutRequest req) {
        auth.logout(parse(userUuid), req);
        return ApiResponse.ok("You have been signed out", "Refresh token revoked", null);
    }

    @GetMapping("/me")
    public ApiResponse<UserProfile> me(@RequestHeader(Headers.USER_UUID) String userUuid) {
        return ApiResponse.ok("Profile loaded", "User profile", auth.profile(parse(userUuid)));
    }

    @GetMapping("/activities")
    public ApiResponse<List<ActivityView>> activities(@RequestHeader(Headers.USER_UUID) String userUuid,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        TLLog.process("LIST_ACTIVITIES");
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<UserActivity> p = activities.list(parse(userUuid), Math.max(page, 0), safeSize);
        List<ActivityView> data = p.getContent().stream()
                .map(a -> new ActivityView(a.getId().toString(), a.getActivityType().name(), a.getDescription(),
                        a.getMetadata(), a.getCreatedAt()))
                .toList();
        return ApiResponse.page("Activities loaded", "User activities", data,
                Pagination.of(p.getNumber(), p.getSize(), p.getTotalElements()));
    }

    /** Called by the gateway before a game is rendered in the WebView. */
    @GetMapping("/validate")
    public ApiResponse<ValidationResult> validate(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                  @RequestHeader(value = Headers.USER_UUID, required = false) String userUuid,
                                                  @RequestHeader(value = Headers.GAME_ID, required = false) String gameId) {
        ValidationResult r = auth.validate(JwtTokenService.extractBearer(authorization), userUuid, gameId);
        return ApiResponse.ok("You're good to go!", "Credentials valid", r);
    }

    private ApiResponse<AuthSession> socialResponse(AuthSession s) {
        String msg = switch (s.sessionType()) {
            case "UPGRADED_GUEST" -> "Your account is ready! Progress saved.";
            case "SIGNED_UP" -> "Welcome to Tiny Leaps!";
            default -> "Welcome back!";
        };
        return new ApiResponse<>(HttpStatus.OK.value(), msg, "Signed in (" + s.sessionType() + ")", s, null);
    }

    private static UUID parse(String uuid) {
        try {
            return UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Invalid " + Headers.USER_UUID);
        }
    }
}
