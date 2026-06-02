DELETE FROM quote_metadata_tags
WHERE tag_id IN (
    SELECT id
    FROM tags
    WHERE type = 'AVOID'
);

DELETE FROM tags
WHERE type = 'AVOID';

ALTER TABLE tags
DROP CONSTRAINT IF EXISTS chk_tag_type;

ALTER TABLE tags
    ADD CONSTRAINT chk_tag_type
        CHECK (type IN (
                        'EMOTION',
                        'NEED',
                        'SITUATION',
                        'CONTEXT',
                        'MOOD',
                        'ROLE'
            ));
