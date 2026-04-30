CREATE SEQUENCE users_id_seq
    START WITH 1
    INCREMENT BY 50;

CREATE TABLE users (
    id BIGINT PRIMARY KEY DEFAULT nextval('users_id_seq'),
    provider VARCHAR(20) NOT NULL,
    provider_id VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    nickname VARCHAR(15) NOT NULL,
    profile_image_id BIGINT,
    status VARCHAR(20) NOT NULL,
    last_login_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT users_provider_check CHECK (provider IN ('KAKAO', 'GOOGLE')),
    CONSTRAINT users_provider_provider_id_unique UNIQUE (provider, provider_id),
    CONSTRAINT users_nickname_check CHECK (
        char_length(nickname) BETWEEN 1 AND 15
        AND nickname !~ '\s'
    ),
    CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'BLOCKED', 'DELETED'))
);

CREATE INDEX users_status_idx ON users (status);

CREATE INDEX users_deleted_at_idx ON users (deleted_at);
