CREATE TABLE users (
    id               UUID PRIMARY KEY,
    account_type     VARCHAR(20)  NOT NULL,           -- GUEST / REGISTERED
    email            VARCHAR(255),
    password         VARCHAR(255),                    -- reserved for email/password login; null for social/guest
    display_name     VARCHAR(255),
    auth_provider    VARCHAR(20),                     -- GOOGLE / APPLE (null for guests)
    provider_subject VARCHAR(255),
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMPTZ  NOT NULL,
    last_active_at   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_provider UNIQUE (auth_provider, provider_subject)
);

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users (id),
    token_hash  VARCHAR(64)  NOT NULL,
    device_id   VARCHAR(128),
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    replaced_by UUID,
    created_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

CREATE TABLE guest_devices (
    id           UUID PRIMARY KEY,
    device_id    VARCHAR(128) NOT NULL,
    user_id      UUID         NOT NULL REFERENCES users (id),
    platform     VARCHAR(20),
    created_at   TIMESTAMPTZ  NOT NULL,
    last_seen_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_guest_devices_device UNIQUE (device_id)
);
CREATE INDEX idx_guest_devices_user ON guest_devices (user_id);

CREATE TABLE user_activities (
    id            UUID PRIMARY KEY,
    user_id       UUID         NOT NULL REFERENCES users (id),
    activity_type VARCHAR(40)  NOT NULL,
    description   VARCHAR(500),
    metadata      TEXT,
    device_id     VARCHAR(128),
    created_at    TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_user_activities_user ON user_activities (user_id, created_at DESC);
