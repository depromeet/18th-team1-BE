CREATE UNIQUE INDEX IF NOT EXISTS uq_diaries_user_day
    ON diaries (user_id, (created_at::date))
    WHERE deleted_at IS NULL;
