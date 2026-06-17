CREATE TABLE IF NOT EXISTS oauth_accounts (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_id VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    provider_display_name VARCHAR(100),
    last_login_at TIMESTAMP,
    disconnected_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT oauth_accounts_provider_check CHECK (provider IN ('KAKAO', 'GOOGLE'))
);

CREATE UNIQUE INDEX IF NOT EXISTS oauth_accounts_provider_provider_id_active_uidx
    ON oauth_accounts (provider, provider_id)
    WHERE disconnected_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS oauth_accounts_user_provider_active_uidx
    ON oauth_accounts (user_id, provider)
    WHERE disconnected_at IS NULL;

CREATE INDEX IF NOT EXISTS oauth_accounts_user_id_idx
    ON oauth_accounts (user_id);

CREATE INDEX IF NOT EXISTS oauth_accounts_disconnected_at_idx
    ON oauth_accounts (disconnected_at);
