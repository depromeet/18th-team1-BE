CREATE TABLE IF NOT EXISTS quote_candidates (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    book_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    source VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reject_reason VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_quote_candidate_status
        CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS quote_candidates_book_content_uidx
    ON quote_candidates (book_id, content);

CREATE INDEX IF NOT EXISTS quote_candidates_book_status_idx
    ON quote_candidates (book_id, status);

CREATE INDEX IF NOT EXISTS quote_candidates_status_idx
    ON quote_candidates (status);
