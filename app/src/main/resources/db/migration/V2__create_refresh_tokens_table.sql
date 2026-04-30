CREATE SEQUENCE refresh_tokens_id_seq
    START WITH 1
    INCREMENT BY 50;

CREATE TABLE refresh_tokens (
    id BIGINT PRIMARY KEY DEFAULT nextval('refresh_tokens_id_seq'),
    user_id BIGINT NOT NULL,
    device_id VARCHAR(36) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT refresh_tokens_token_hash_unique UNIQUE (token_hash),
    CONSTRAINT refresh_tokens_user_device_unique UNIQUE (user_id, device_id)
);

CREATE INDEX refresh_tokens_user_id_idx ON refresh_tokens (user_id);

CREATE INDEX refresh_tokens_expires_at_idx ON refresh_tokens (expires_at);
