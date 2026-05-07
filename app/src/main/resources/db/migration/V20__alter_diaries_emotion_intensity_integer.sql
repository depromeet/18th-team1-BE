ALTER TABLE diaries
    ALTER COLUMN emotion_intensity TYPE INTEGER
    USING emotion_intensity::INTEGER;
