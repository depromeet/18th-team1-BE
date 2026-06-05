CREATE TABLE IF NOT EXISTS quote_scraps (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    quote_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS quote_scraps_user_quote_uidx
    ON quote_scraps (user_id, quote_id);

CREATE INDEX IF NOT EXISTS quote_scraps_quote_id_idx
    ON quote_scraps (quote_id);

CREATE INDEX IF NOT EXISTS daily_recommendations_quote_id_idx
    ON daily_recommendations (quote_id);

CREATE INDEX IF NOT EXISTS daily_recommendation_quotes_quote_id_idx
    ON daily_recommendation_quotes (quote_id);
