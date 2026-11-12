package com.tinyleaps.auth.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.tinyleaps.auth.config.AuthProperties;
import com.tinyleaps.auth.entity.AuthProvider;
import com.tinyleaps.common.api.ApiException;
import com.tinyleaps.common.logging.TLLog;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verifies Google / Apple identity tokens (signature via the provider's JWKS, issuer, audience, expiry).
 */
@Component
public class SocialIdentityVerifier {

    public record SocialIdentity(AuthProvider provider, String subject, String email, boolean emailVerified, String name) {}

    private record ProviderSpec(String jwksUrl, Set<String> issuers) {}

    private static final Map<AuthProvider, ProviderSpec> SPECS = Map.of(
            AuthProvider.GOOGLE, new ProviderSpec("https://www.googleapis.com/oauth2/v3/certs",
                    Set.of("https://accounts.google.com", "accounts.google.com")),
            AuthProvider.APPLE, new ProviderSpec("https://appleid.apple.com/auth/keys",
                    Set.of("https://appleid.apple.com")));

    private final AuthProperties props;
    private final Map<AuthProvider, DefaultJWTProcessor<SecurityContext>> processors = new ConcurrentHashMap<>();

    public SocialIdentityVerifier(AuthProperties props) {
        this.props = props;
    }

    public SocialIdentity verify(AuthProvider provider, String idToken) {
        List<String> audiences = provider == AuthProvider.GOOGLE
                ? props.social().google().clientIdsOrEmpty()
                : props.social().apple().clientIdsOrEmpty();
        if (audiences.isEmpty()) {
            throw new ApiException(503, provider.name().charAt(0) + provider.name().substring(1).toLowerCase()
                    + " sign-in is not available right now.", provider + " client id(s) not configured on auth-service");
        }
        try {
            JWTClaimsSet c = processors.computeIfAbsent(provider, p -> buildProcessor(p, audiences)).process(idToken, null);
            if (!SPECS.get(provider).issuers().contains(c.getIssuer())) {
                throw ApiException.unauthorized(provider + "_TOKEN_INVALID: unexpected issuer " + c.getIssuer());
            }
            Object verified = c.getClaim("email_verified");
            boolean emailVerified = Boolean.TRUE.equals(verified) || "true".equals(String.valueOf(verified));
            return new SocialIdentity(provider, c.getSubject(), c.getStringClaim("email"), emailVerified,
                    c.getClaims().containsKey("name") ? c.getStringClaim("name") : null);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            TLLog.warn("SOCIAL_TOKEN_VERIFY", provider + " token rejected: " + e.getMessage());
            throw ApiException.unauthorized(provider + "_TOKEN_INVALID: " + e.getMessage());
        }
    }

    private DefaultJWTProcessor<SecurityContext> buildProcessor(AuthProvider provider, List<String> audiences) {
        try {
            JWKSource<SecurityContext> keys = JWKSourceBuilder
                    .create(URI.create(SPECS.get(provider).jwksUrl()).toURL())
                    .retrying(true)
                    .build();
            DefaultJWTProcessor<SecurityContext> p = new DefaultJWTProcessor<>();
            p.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keys));
            p.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(
                    new HashSet<>(audiences), null, Set.of("sub", "iss", "exp", "aud"), null));
            return p;
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }
}
