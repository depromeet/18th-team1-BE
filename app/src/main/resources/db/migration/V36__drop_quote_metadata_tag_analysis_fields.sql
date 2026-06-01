ALTER TABLE quote_metadata_tags
    DROP CONSTRAINT IF EXISTS chk_quote_metadata_tags_confidence;

ALTER TABLE quote_metadata_tags
    DROP CONSTRAINT IF EXISTS chk_quote_metadata_tags_priority;

ALTER TABLE quote_metadata_tags
    DROP COLUMN IF EXISTS evidence,
    DROP COLUMN IF EXISTS confidence,
    DROP COLUMN IF EXISTS priority;
