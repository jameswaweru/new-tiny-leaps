CREATE TABLE virtual_games (
    id             UUID PRIMARY KEY,
    game_id        VARCHAR(64)  NOT NULL,   -- unique public id; rendered at {gamesBaseUrl}/{game_id}
    title          VARCHAR(120) NOT NULL,
    description    VARCHAR(500) NOT NULL,
    category       VARCHAR(40)  NOT NULL,   -- PUZZLE / PAINTING / ...
    min_age        INT          NOT NULL,
    max_age        INT          NOT NULL,
    themes         VARCHAR(255),            -- comma separated themes available inside the game
    skills         VARCHAR(255),            -- comma separated skills the game develops
    thumbnail_path VARCHAR(255) NOT NULL,
    theme_color    VARCHAR(9)   NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    sort_order     INT          NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_virtual_games_game_id UNIQUE (game_id),
    CONSTRAINT ck_virtual_games_age CHECK (min_age >= 0 AND max_age >= min_age)
);
CREATE INDEX idx_virtual_games_status_sort ON virtual_games (status, sort_order);
