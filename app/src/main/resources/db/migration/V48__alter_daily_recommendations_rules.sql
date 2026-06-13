DROP INDEX IF EXISTS daily_recommendations_user_date_uidx;
DROP INDEX IF EXISTS daily_recommendations_quote_id_idx;
DROP INDEX IF EXISTS daily_recommendation_quotes_quote_id_idx;
DROP INDEX IF EXISTS daily_recommendation_quotes_recommendation_quote_uidx;
DROP INDEX IF EXISTS daily_recommendation_quotes_recommendation_display_order_uidx;
DROP INDEX IF EXISTS daily_recommendation_tags_recommendation_tag_uidx;
DROP INDEX IF EXISTS quote_batch_jobs_single_running_uidx;

ALTER TABLE daily_recommendations
    RENAME COLUMN selected_emotion_range_id TO emotion_range_id;

ALTER TABLE IF EXISTS daily_recommendations
    ADD COLUMN IF NOT EXISTS feeling_text TEXT;

ALTER TABLE IF EXISTS daily_recommendations
    ADD COLUMN IF NOT EXISTS diary_text TEXT;

ALTER TABLE IF EXISTS recommendations
    ADD COLUMN IF NOT EXISTS feeling_text TEXT;

ALTER TABLE IF EXISTS recommendations
    ADD COLUMN IF NOT EXISTS diary_text TEXT;

ALTER TABLE IF EXISTS daily_recommendations
    ALTER COLUMN feeling_text TYPE TEXT;

ALTER TABLE IF EXISTS recommendations
    ALTER COLUMN feeling_text TYPE TEXT;

DO $$
BEGIN
    IF to_regclass('public.daily_recommendations') IS NOT NULL
       AND EXISTS (
           SELECT 1
           FROM information_schema.columns
           WHERE table_schema = 'public'
             AND table_name = 'daily_recommendations'
             AND column_name = 'user_context'
       ) THEN
        UPDATE daily_recommendations
        SET feeling_text = user_context
        WHERE user_context IS NOT NULL
          AND feeling_text IS NULL;
    END IF;

    IF to_regclass('public.recommendations') IS NOT NULL
       AND EXISTS (
           SELECT 1
           FROM information_schema.columns
           WHERE table_schema = 'public'
             AND table_name = 'recommendations'
             AND column_name = 'user_context'
       ) THEN
        UPDATE recommendations
        SET feeling_text = user_context
        WHERE user_context IS NOT NULL
          AND feeling_text IS NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF to_regclass('public.recommendations') IS NOT NULL
       AND to_regclass('public.diaries') IS NOT NULL THEN
        UPDATE recommendations AS recommendation
        SET feeling_text = recommendation.diary_text
        FROM diaries AS diary
        WHERE diary.user_id = recommendation.user_id
          AND diary.created_at::date = recommendation.recommendation_date
          AND diary.deleted_at IS NULL
          AND recommendation.feeling_text IS NULL
          AND recommendation.diary_text IS NOT NULL
          AND recommendation.diary_text IS DISTINCT FROM diary.content;
    END IF;
END $$;

DO $$
BEGIN
    IF to_regclass('public.daily_recommendations') IS NOT NULL
       AND to_regclass('public.diaries') IS NOT NULL THEN
        UPDATE daily_recommendations AS daily_recommendation
        SET diary_text = diary.content
        FROM diaries AS diary
        WHERE diary.user_id = daily_recommendation.user_id
          AND diary.created_at::date = daily_recommendation.recommendation_date
          AND diary.deleted_at IS NULL;
    END IF;

    IF to_regclass('public.recommendations') IS NOT NULL
       AND to_regclass('public.diaries') IS NOT NULL THEN
        UPDATE recommendations AS recommendation
        SET diary_text = diary.content
        FROM diaries AS diary
        WHERE diary.user_id = recommendation.user_id
          AND diary.created_at::date = recommendation.recommendation_date
          AND diary.deleted_at IS NULL;
    END IF;
END $$;

ALTER TABLE IF EXISTS daily_recommendations
    DROP COLUMN IF EXISTS user_context;

ALTER TABLE IF EXISTS recommendations
    DROP COLUMN IF EXISTS user_context;

DO $$
BEGIN
    IF to_regclass('public.daily_recommendation_quotes') IS NOT NULL
       AND EXISTS (
           SELECT 1
           FROM information_schema.columns
           WHERE table_schema = 'public'
             AND table_name = 'daily_recommendation_quotes'
             AND column_name = 'daily_recommendation_id'
       )
       AND NOT EXISTS (
           SELECT 1
           FROM information_schema.columns
           WHERE table_schema = 'public'
             AND table_name = 'daily_recommendation_quotes'
             AND column_name = 'recommendation_id'
       ) THEN
        ALTER TABLE daily_recommendation_quotes
            RENAME COLUMN daily_recommendation_id TO recommendation_id;
    END IF;

    IF to_regclass('public.daily_recommendation_tags') IS NOT NULL
       AND EXISTS (
           SELECT 1
           FROM information_schema.columns
           WHERE table_schema = 'public'
             AND table_name = 'daily_recommendation_tags'
             AND column_name = 'daily_recommendation_id'
       )
       AND NOT EXISTS (
           SELECT 1
           FROM information_schema.columns
           WHERE table_schema = 'public'
             AND table_name = 'daily_recommendation_tags'
             AND column_name = 'recommendation_id'
       ) THEN
        ALTER TABLE daily_recommendation_tags
            RENAME COLUMN daily_recommendation_id TO recommendation_id;
    END IF;
END $$;

DO $$
BEGIN
    IF to_regclass('public.daily_recommendations') IS NOT NULL
       AND to_regclass('public.recommendations') IS NULL THEN
        ALTER TABLE daily_recommendations
            RENAME TO recommendations;
    END IF;

    IF to_regclass('public.daily_recommendation_quotes') IS NOT NULL
       AND to_regclass('public.recommendation_quotes') IS NULL THEN
        ALTER TABLE daily_recommendation_quotes
            RENAME TO recommendation_quotes;
    END IF;

    IF to_regclass('public.daily_recommendation_tags') IS NOT NULL
       AND to_regclass('public.recommendation_tags') IS NULL THEN
        ALTER TABLE daily_recommendation_tags
            RENAME TO recommendation_tags;
    END IF;
END $$;

ALTER TABLE IF EXISTS recommendations
    ALTER COLUMN quote_id DROP NOT NULL;

CREATE INDEX IF NOT EXISTS recommendations_user_date_idx
    ON recommendations (user_id, recommendation_date);

CREATE INDEX IF NOT EXISTS recommendations_quote_id_idx
    ON recommendations (quote_id);

CREATE INDEX IF NOT EXISTS recommendation_quotes_quote_id_idx
    ON recommendation_quotes (quote_id);

CREATE UNIQUE INDEX IF NOT EXISTS recommendation_quotes_recommendation_quote_uidx
    ON recommendation_quotes (recommendation_id, quote_id);

CREATE UNIQUE INDEX IF NOT EXISTS recommendation_quotes_recommendation_display_order_uidx
    ON recommendation_quotes (recommendation_id, display_order);

CREATE UNIQUE INDEX IF NOT EXISTS recommendation_tags_recommendation_tag_uidx
    ON recommendation_tags (recommendation_id, tag_id);

DROP TABLE IF EXISTS diary_tags;

DROP TABLE IF EXISTS diaries;
