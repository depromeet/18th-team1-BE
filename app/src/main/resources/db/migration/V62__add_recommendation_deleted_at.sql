ALTER TABLE IF EXISTS recommendations
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS recommendations_active_user_date_idx
    ON recommendations (user_id, recommendation_date)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS recommendations_active_quote_id_idx
    ON recommendations (quote_id)
    WHERE deleted_at IS NULL;
