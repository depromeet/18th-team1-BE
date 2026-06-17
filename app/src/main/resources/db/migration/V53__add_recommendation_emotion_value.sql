ALTER TABLE recommendations
    ADD COLUMN IF NOT EXISTS emotion_value INTEGER;

UPDATE recommendations AS recommendation
SET emotion_value =
    CASE emotion_range.name
        WHEN 'SAD' THEN 1
        WHEN 'NORMAL' THEN 5
        WHEN 'HAPPY' THEN 9
        ELSE LEAST(9, GREATEST(1, ((emotion_range.min_value + emotion_range.max_value) / 2)))
    END
FROM emotion_ranges AS emotion_range
WHERE recommendation.emotion_range_id = emotion_range.id
  AND recommendation.emotion_value IS NULL;

UPDATE recommendations
SET emotion_value = 5
WHERE emotion_value IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND table_name = 'recommendations'
          AND constraint_name = 'recommendations_emotion_value_check'
    ) THEN
        ALTER TABLE recommendations
            ADD CONSTRAINT recommendations_emotion_value_check
                CHECK (emotion_value BETWEEN 1 AND 9);
    END IF;
END $$;

ALTER TABLE recommendations
    ALTER COLUMN emotion_value SET NOT NULL;
