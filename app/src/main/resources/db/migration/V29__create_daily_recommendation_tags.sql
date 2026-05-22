CREATE TABLE IF NOT EXISTS daily_recommendation_tags (
     id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
     daily_recommendation_id BIGINT NOT NULL,
     tag_id BIGINT NOT NULL,
     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS daily_recommendation_tags_recommendation_tag_uidx
    ON daily_recommendation_tags (daily_recommendation_id, tag_id);