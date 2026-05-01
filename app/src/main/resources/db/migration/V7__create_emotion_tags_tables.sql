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

      CONSTRAINT fk_tags_emotion_range
          FOREIGN KEY (emotion_range_id)
              REFERENCES emotion_ranges(id)
              ON DELETE RESTRICT,

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

INSERT INTO emotion_ranges (name, min_value, max_value) VALUES
('SAD', 0, 33),
('NORMAL', 34, 66),
('HAPPY', 67, 100);

-- =========================
-- DATA: tags
-- =========================

-- SAD
INSERT INTO tags (emotion_range_id, label, type) VALUES
(1, '지침', 'EMOTION'), (1, '무기력', 'EMOTION'), (1, '우울', 'EMOTION'),
(1, '슬픔', 'EMOTION'), (1, '외로움', 'EMOTION'), (1, '허무', 'EMOTION'),
(1, '불안', 'EMOTION'), (1, '걱정', 'EMOTION'), (1, '답답함', 'EMOTION'),
(1, '공허함', 'EMOTION'), (1, '의욕없음', 'EMOTION'), (1, '피곤함', 'EMOTION'),
(1, '좌절', 'EMOTION'), (1, '상실감', 'EMOTION'), (1, '고독', 'EMOTION');

-- NORMAL
INSERT INTO tags (emotion_range_id, label, type) VALUES
(2, '평온', 'EMOTION'), (2, '무난', 'EMOTION'), (2, '일상', 'EMOTION'),
(2, '안정', 'EMOTION'), (2, '차분함', 'EMOTION'), (2, '균형', 'EMOTION'),
(2, '보통', 'EMOTION'), (2, '잔잔함', 'EMOTION'), (2, '여유', 'EMOTION'),
(2, '편안함', 'EMOTION'), (2, '무탈함', 'EMOTION'), (2, '담담함', 'EMOTION'),
(2, '중립', 'EMOTION'), (2, '조용함', 'EMOTION'), (2, '평범', 'EMOTION');

-- HAPPY
INSERT INTO tags (emotion_range_id, label, type) VALUES
(3, '행복', 'EMOTION'), (3, '기쁨', 'EMOTION'), (3, '즐거움', 'EMOTION'),
(3, '설렘', 'EMOTION'), (3, '신남', 'EMOTION'), (3, '활기', 'EMOTION'),
(3, '만족', 'EMOTION'), (3, '감사', 'EMOTION'), (3, '희망', 'EMOTION'),
(3, '기대', 'EMOTION'), (3, '자신감', 'EMOTION'), (3, '열정', 'EMOTION'),
(3, '뿌듯함', 'EMOTION'), (3, '상쾌함', 'EMOTION'), (3, '웃음', 'EMOTION');

-- TONE
INSERT INTO tags (emotion_range_id, label, type) VALUES
(NULL, '따뜻한', 'TONE'),
(NULL, '담담한', 'TONE'),
(NULL, '위로하는', 'TONE'),
(NULL, '차분한', 'TONE'),
(NULL, '희망적인', 'TONE'),
(NULL, '현실적인', 'TONE'),
(NULL, '다정한', 'TONE'),
(NULL, '가벼운', 'TONE');
