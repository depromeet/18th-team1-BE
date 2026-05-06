CREATE SEQUENCE emotion_ranges_id_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE tags_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE emotion_ranges (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    min_value INT NOT NULL,
    max_value INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_emotion_range_name
        CHECK (name IN ('SAD', 'NORMAL', 'HAPPY')),

    CONSTRAINT chk_emotion_range_value
        CHECK (min_value <= max_value)
);

CREATE TABLE tags (
      id BIGSERIAL PRIMARY KEY,
      emotion_range_id BIGINT,
      label VARCHAR(50) NOT NULL,
      type VARCHAR(20) NOT NULL,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

      CONSTRAINT chk_tag_type
          CHECK (type IN ('EMOTION', 'TONE')),

      CONSTRAINT chk_emotion_tag_range
          CHECK (
              (type = 'EMOTION' AND emotion_range_id IS NOT NULL)
                  OR
              (type = 'TONE' AND emotion_range_id IS NULL)
              )
);

CREATE INDEX tags_emotion_range_id_idx ON tags (emotion_range_id);
CREATE INDEX tags_type_idx ON tags (type);
