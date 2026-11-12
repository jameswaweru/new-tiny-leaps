package com.tinyleaps.auth.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_activities")
public class UserActivity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private ActivityType activityType;

    private String description;

    private String metadata;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserActivity() {}

    public UserActivity(UUID userId, ActivityType type, String description, String metadata, String deviceId) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.activityType = type;
        this.description = description;
        this.metadata = metadata;
        this.deviceId = deviceId;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public ActivityType getActivityType() { return activityType; }
    public String getDescription() { return description; }
    public String getMetadata() { return metadata; }
    public Instant getCreatedAt() { return createdAt; }
}
