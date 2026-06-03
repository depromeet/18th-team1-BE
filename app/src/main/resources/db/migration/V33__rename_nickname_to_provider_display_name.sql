ALTER TABLE users
    DROP CONSTRAINT IF EXISTS users_nickname_check;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS provider_display_name VARCHAR(100);

UPDATE users
SET provider_display_name = nickname
WHERE provider_display_name IS NULL;

ALTER TABLE users
    ALTER COLUMN provider_display_name TYPE VARCHAR(100);

ALTER TABLE users
    ALTER COLUMN provider_display_name SET NOT NULL,
    ALTER COLUMN nickname SET NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT users_nickname_check CHECK (
        char_length(nickname) BETWEEN 1 AND 15
        AND nickname !~ '\s'
    );

CREATE UNIQUE INDEX users_nickname_unique_idx
    ON users (nickname)
    WHERE deleted_at IS NULL;
