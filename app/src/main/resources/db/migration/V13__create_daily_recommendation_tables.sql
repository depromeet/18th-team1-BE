CREATE TABLE daily_recommendations (
   id BIGSERIAL PRIMARY KEY,
   user_id BIGINT NOT NULL,
   quote_id BIGINT NOT NULL,
   recommendation_date DATE NOT NULL,
   user_context TEXT NOT NULL,
   selected_emotion_range_id BIGINT NOT NULL,
   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX daily_recommendations_user_date_uidx
    ON daily_recommendations (user_id, recommendation_date);
