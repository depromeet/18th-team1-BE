ALTER TABLE tags
DROP CONSTRAINT IF EXISTS chk_tag_type;

ALTER TABLE tags
DROP CONSTRAINT IF EXISTS chk_emotion_tag_range;

ALTER TABLE tags
    ADD COLUMN IF NOT EXISTS code VARCHAR(50),
    ADD COLUMN IF NOT EXISTS sort_order INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE tags
SET type = 'NEED'
WHERE type = 'TONE';

ALTER TABLE tags
    ADD CONSTRAINT chk_tag_type
        CHECK (type IN (
                        'EMOTION',
                        'NEED',
                        'SITUATION',
                        'CONTEXT',
                        'MOOD',
                        'ROLE',
                        'AVOID'
            ));

ALTER TABLE tags
    ADD CONSTRAINT chk_emotion_tag_range
        CHECK (
            (type = 'EMOTION' AND emotion_range_id IS NOT NULL)
                OR
            (type <> 'EMOTION' AND emotion_range_id IS NULL)
            );

CREATE UNIQUE INDEX IF NOT EXISTS tags_type_code_uidx
    ON tags (type, code);
