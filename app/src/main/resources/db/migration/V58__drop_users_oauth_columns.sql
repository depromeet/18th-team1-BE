ALTER TABLE users
    DROP CONSTRAINT IF EXISTS users_provider_provider_id_unique,
    DROP CONSTRAINT IF EXISTS users_provider_check;

ALTER TABLE users
    DROP COLUMN IF EXISTS provider,
    DROP COLUMN IF EXISTS provider_id,
    DROP COLUMN IF EXISTS email,
    DROP COLUMN IF EXISTS provider_display_name,
    DROP COLUMN IF EXISTS last_login_at;
