DELETE FROM tags;

ALTER TABLE tags
    ALTER COLUMN id RESTART WITH 1;

INSERT INTO tags (emotion_range_id, label, type)
VALUES
    ((SELECT id FROM emotion_ranges WHERE name = 'SAD'), '지쳐있는 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'SAD'), '아무것도 하기 싫은 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'SAD'), '눈물이 날 것 같은 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'SAD'), '위축되는 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'SAD'), '억울한 마음이 드는 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'SAD'), '자책하고 싶은 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'SAD'), '포기하고 싶은 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'SAD'), '한없이 우울한 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'SAD'), '불안해지는 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'SAD'), '크게 실망하게 되는 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'SAD'), '앞이 막막한 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'SAD'), '짜증이 밀려오는 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'SAD'), '서운해지는 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'SAD'), '마음이 공허한 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'SAD'), '후회가 밀려오는 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'SAD'), '신경이 예민해지는 순간', 'EMOTION');

INSERT INTO tags (emotion_range_id, label, type)
VALUES
    ((SELECT id FROM emotion_ranges WHERE name = 'NORMAL'), '심심한 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'NORMAL'), '혼자 있고 싶은 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'NORMAL'), '마음이 차분해지는 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'NORMAL'), '멍해지는 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'NORMAL'), '감정이 무덤덤해지는 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'NORMAL'), '평온한 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'NORMAL'), '몸이 나른해지는 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'NORMAL'), '생각에 잠기는 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'NORMAL'), '마음이 싱숭생숭한 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'NORMAL'), '여유가 느껴지는 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'NORMAL'), '평범하게 흘러가는 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'NORMAL'), '기분이 애매한 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'NORMAL'), '문득 센치해지는 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'NORMAL'), '무난하게 흘러가는 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'NORMAL'), '마음이 너그러워지는 순간', 'EMOTION');

INSERT INTO tags (emotion_range_id, label, type)
VALUES
    ((SELECT id FROM emotion_ranges WHERE name = 'HAPPY'), '뿌듯한 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'HAPPY'), '새로운 걸 시작한 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'HAPPY'), '기분 좋게 웃은 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'HAPPY'), '더없이 행복한 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'HAPPY'), '신나는 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'HAPPY'), '설레는 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'HAPPY'), '홀가분한 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'HAPPY'), '짜릿함이 느껴지는 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'HAPPY'), '한없이 평화로운 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'HAPPY'), '감사함이 느껴지는 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'HAPPY'), '안도하는 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'HAPPY'), '충만한 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'HAPPY'), '에너지가 돌고 활기찬 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'HAPPY'), '감격스러운 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'HAPPY'), '자신감이 넘치는 순간', 'EMOTION'),
    ((SELECT id FROM emotion_ranges WHERE name = 'HAPPY'), '열정이 타오르는 순간', 'EMOTION');

INSERT INTO tags (emotion_range_id, label, type)
VALUES
    (NULL, '위로가 되는 문장', 'TONE'),
    (NULL, '공감해주는 문장', 'TONE'),
    (NULL, '영감을 주는 문장', 'TONE');
