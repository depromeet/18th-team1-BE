ALTER TABLE quotes
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(30) NOT NULL DEFAULT 'MANUAL';

ALTER TABLE quotes
    DROP CONSTRAINT IF EXISTS chk_quotes_source_type;

ALTER TABLE quotes
    ADD CONSTRAINT chk_quotes_source_type
        CHECK (source_type IN ('MANUAL', 'WEB_EXTRACTED', 'BOOK_INSPIRED', 'CANDIDATE_REVIEWED'));

CREATE INDEX IF NOT EXISTS quotes_source_type_idx
    ON quotes (source_type);
