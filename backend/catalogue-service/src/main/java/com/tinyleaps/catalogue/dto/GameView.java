package com.tinyleaps.catalogue.dto;

import java.util.List;

/**
 * Catalogue entry as returned to the app. {@code launchPath} and {@code thumbnailPath} are relative to the
 * gateway base URL; the game is rendered at {@code {baseUrl}/games/{gameId}}.
 */
public record GameView(String gameId,
                       String title,
                       String description,
                       String category,
                       int minAge,
                       int maxAge,
                       String ageLabel,
                       List<String> themes,
                       List<String> skills,
                       String thumbnailPath,
                       String launchPath,
                       String themeColor) {}
