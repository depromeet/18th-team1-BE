DELETE FROM image_owners WHERE owner_type = 'DIARY';
DELETE FROM diary_tags;
DELETE FROM diaries;

ALTER TABLE diaries
    ALTER COLUMN emotion_intensity TYPE INTEGER
    USING emotion_intensity::INTEGER;
