package com.tinyleaps.auth.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** SHA-256 hex of the opaque token; the raw token is never stored. */
    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    @Column(name = "replaced_by")
    private UUID replacedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RefreshToken() {}

    public RefreshToken(UUID userId, String tokenHash, String deviceId, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.deviceId = deviceId;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public boolean isExpired() { return expiresAt.isBefore(Instant.now()); }
    public void revoke(UUID replacedBy) { this.revoked = true; this.replacedBy = replacedBy; }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getDeviceId() { return deviceId; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isRevoked() { return revoked; }
}
