package com.tinyleaps.games.painting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Standalone game service for catalogue game id {@code tl-paint-01}. */
@SpringBootApplication
public class PaintingGameApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaintingGameApplication.class, args);
    }
}
