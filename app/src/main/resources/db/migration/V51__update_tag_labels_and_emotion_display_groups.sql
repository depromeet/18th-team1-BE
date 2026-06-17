ALTER TABLE IF EXISTS tags
    ADD COLUMN IF NOT EXISTS display_group VARCHAR(50);

WITH need_tag_labels(id, label, sort_order) AS (
    VALUES
        (51, '관점을 전환하는 문장', 1),
        (106, '마음정리를 돕는 문장', 2),
        (78, '시작을 응원하는 문장', 3),
        (50, '영감을 주는 문장', 4),
        (49, '공감해주는 문장', 5),
        (48, '위로를 주는 문장', 6),
        (105, '자존감을 채우는 문장', 7),
        (121, '마음의 짐을 덜어주는 문장', 8),
        (111, '용기를 주는 문장', 9)
)
UPDATE tags
SET
    label = need_tag_labels.label,
    sort_order = need_tag_labels.sort_order
FROM need_tag_labels
WHERE tags.id = need_tag_labels.id
  AND tags.type = 'NEED';

WITH emotion_tag_groups(id, label, display_group, sort_order) AS (
    VALUES
        (4, NULL, '무기력과 우울', 1),
        (6, NULL, '무기력과 우울', 2),
        (9, NULL, '무기력과 우울', 3),
        (10, NULL, '무기력과 우울', 4),
        (15, NULL, '무기력과 우울', 5),
        (1, NULL, '불안과 위축', 6),
        (2, NULL, '불안과 위축', 7),
        (3, NULL, '불안과 위축', 8),
        (8, NULL, '불안과 위축', 9),
        (11, NULL, '불안과 위축', 10),
        (14, NULL, '불안과 위축', 11),
        (5, NULL, '분노와 상처', 12),
        (7, NULL, '분노와 상처', 13),
        (13, NULL, '분노와 상처', 14),
        (12, NULL, '분노와 상처', 15),
        (16, NULL, '분노와 상처', 16),
        (19, NULL, '평온과 여유', 1),
        (22, NULL, '평온과 여유', 2),
        (26, NULL, '평온과 여유', 3),
        (31, NULL, '평온과 여유', 4),
        (27, NULL, '평온과 여유', 5),
        (18, NULL, '사색과 고립', 6),
        (24, NULL, '사색과 고립', 7),
        (29, NULL, '사색과 고립', 8),
        (17, NULL, '사색과 고립', 9),
        (23, NULL, '사색과 고립', 10),
        (20, NULL, '무감각과 모호함', 11),
        (21, NULL, '무감각과 모호함', 12),
        (28, NULL, '무감각과 모호함', 13),
        (25, NULL, '무감각과 모호함', 14),
        (35, NULL, '행복하고 충만한', 1),
        (34, '기분 좋은', '행복하고 충만한', 2),
        (32, NULL, '행복하고 충만한', 3),
        (41, NULL, '행복하고 충만한', 4),
        (45, NULL, '행복하고 충만한', 5),
        (43, NULL, '행복하고 충만한', 6),
        (33, NULL, '열정과 활기', 7),
        (36, NULL, '열정과 활기', 8),
        (37, NULL, '열정과 활기', 9),
        (39, NULL, '열정과 활기', 10),
        (44, NULL, '열정과 활기', 11),
        (46, NULL, '열정과 활기', 12),
        (47, NULL, '열정과 활기', 13),
        (38, NULL, '안도와 평화', 14),
        (40, NULL, '안도와 평화', 15),
        (42, NULL, '안도와 평화', 16)
)
UPDATE tags
SET
    label = COALESCE(emotion_tag_groups.label, tags.label),
    display_group = emotion_tag_groups.display_group,
    sort_order = emotion_tag_groups.sort_order
FROM emotion_tag_groups
WHERE tags.id = emotion_tag_groups.id
  AND tags.type = 'EMOTION';
