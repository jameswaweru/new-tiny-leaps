package com.tinyleaps.auth.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "guest_devices")
public class GuestDevice {

    @Id
    private UUID id;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    private String platform;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    protected GuestDevice() {}

    public GuestDevice(String deviceId, UUID userId, String platform) {
        this.id = UUID.randomUUID();
        this.deviceId = deviceId;
        this.userId = userId;
        this.platform = platform;
        this.createdAt = Instant.now();
        this.lastSeenAt = createdAt;
    }

    public void relink(UUID userId) { this.userId = userId; seen(); }
    public void seen() { this.lastSeenAt = Instant.now(); }

    public UUID getId() { return id; }
    public String getDeviceId() { return deviceId; }
    public UUID getUserId() { return userId; }
    public String getPlatform() { return platform; }
}
