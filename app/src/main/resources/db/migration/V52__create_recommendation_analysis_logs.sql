CREATE TABLE IF NOT EXISTS recommendation_analysis_logs (
    id BIGSERIAL PRIMARY KEY,
    recommendation_id BIGINT NOT NULL,
    llm_model VARCHAR(80),
    llm_model_version INTEGER,
    canonical_intent TEXT,
    embedding_input_text TEXT,
    prompt_cache_hit BOOLEAN NOT NULL DEFAULT FALSE,
    input_tokens BIGINT,
    cached_tokens BIGINT,
    output_tokens BIGINT,
    llm_elapsed_ms BIGINT,
    embedding_elapsed_ms BIGINT,
    created_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS recommendation_analysis_logs_recommendation_uidx
    ON recommendation_analysis_logs (recommendation_id);
