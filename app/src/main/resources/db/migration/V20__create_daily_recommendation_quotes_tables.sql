CREATE TABLE daily_recommendation_quotes (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 daily_recommendation_id BIGINT NOT NULL,
 quote_id BIGINT NOT NULL,
 display_order INT NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX daily_recommendation_quotes_recommendation_quote_uidx
    ON daily_recommendation_quotes (daily_recommendation_id, quote_id);

CREATE UNIQUE INDEX daily_recommendation_quotes_recommendation_display_order_uidx
    ON daily_recommendation_quotes (daily_recommendation_id, display_order);
