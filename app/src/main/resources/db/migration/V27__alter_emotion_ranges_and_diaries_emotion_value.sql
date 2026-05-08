UPDATE emotion_ranges
SET min_value = 1,
    max_value = 3
WHERE name = 'SAD';

UPDATE emotion_ranges
SET min_value = 4,
    max_value = 6
WHERE name = 'NORMAL';

UPDATE emotion_ranges
SET min_value = 7,
    max_value = 9
WHERE name = 'HAPPY';

ALTER TABLE diaries
    RENAME COLUMN emotion_intensity TO emotion_value;

UPDATE diaries
SET emotion_value = LEAST(9, GREATEST(1, CEIL(emotion_value * 9.0 / 100)::INTEGER))
WHERE emotion_value NOT BETWEEN 1 AND 9;

ALTER TABLE diaries
    ADD CONSTRAINT diaries_emotion_value_check
        CHECK (emotion_value BETWEEN 1 AND 9);
