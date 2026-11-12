package com.tinyleaps.games.puzzle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Standalone game service for catalogue game id {@code tl-puzzle-01}. */
@SpringBootApplication
public class PuzzleGameApplication {
    public static void main(String[] args) {
        SpringApplication.run(PuzzleGameApplication.class, args);
    }
}
