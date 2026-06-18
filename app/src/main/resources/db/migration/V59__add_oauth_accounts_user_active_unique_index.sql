CREATE UNIQUE INDEX IF NOT EXISTS oauth_accounts_user_active_uidx
    ON oauth_accounts (user_id)
    WHERE disconnected_at IS NULL;
