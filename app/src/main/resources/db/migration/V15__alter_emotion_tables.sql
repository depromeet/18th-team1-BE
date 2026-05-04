ALTER TABLE emotion_ranges
    ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE tags
    ALTER COLUMN created_at SET NOT NULL;