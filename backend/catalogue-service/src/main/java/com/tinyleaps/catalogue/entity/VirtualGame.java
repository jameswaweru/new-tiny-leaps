package com.tinyleaps.catalogue.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "virtual_games")
public class VirtualGame {

    @Id
    private UUID id;

    @Column(name = "game_id", nullable = false, unique = true)
    private String gameId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String category;

    @Column(name = "min_age", nullable = false)
    private int minAge;

    @Column(name = "max_age", nullable = false)
    private int maxAge;

    private String themes;

    private String skills;

    @Column(name = "thumbnail_path", nullable = false)
    private String thumbnailPath;

    @Column(name = "theme_color", nullable = false)
    private String themeColor;

    @Column(nullable = false)
    private String status;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected VirtualGame() {}

    public UUID getId() { return id; }
    public String getGameId() { return gameId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public int getMinAge() { return minAge; }
    public int getMaxAge() { return maxAge; }
    public String getThemes() { return themes; }
    public String getSkills() { return skills; }
    public String getThumbnailPath() { return thumbnailPath; }
    public String getThemeColor() { return themeColor; }
    public String getStatus() { return status; }
    public int getSortOrder() { return sortOrder; }
}
