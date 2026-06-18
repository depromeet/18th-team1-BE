CREATE TABLE IF NOT EXISTS genres (
    id BIGINT PRIMARY KEY,
    label VARCHAR(100) NOT NULL,
    sort_order INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS genres_label_uidx ON genres (label);

CREATE UNIQUE INDEX IF NOT EXISTS genres_sort_order_uidx ON genres (sort_order);

INSERT INTO genres (id, label, sort_order)
VALUES
    (1, '일반문학', 1),
    (2, 'SF', 2),
    (3, '추리･미스터리', 3),
    (4, '공포･스릴러', 4),
    (5, '판타지', 5),
    (6, '로맨스', 6),
    (7, '역사', 7),
    (8, '무협', 8),
    (9, '시･에세이', 9)
ON CONFLICT (id) DO UPDATE
SET label = EXCLUDED.label,
    sort_order = EXCLUDED.sort_order;

ALTER TABLE books
    ADD COLUMN IF NOT EXISTS genre_id BIGINT;

UPDATE books
SET genre_id = genres.id
FROM genres
WHERE books.genre = genres.label
  AND books.genre_id IS NULL;

UPDATE books
SET genre_id = 1
WHERE genre_id IS NULL;

ALTER TABLE books
    ALTER COLUMN genre_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS books_genre_id_idx ON books (genre_id);
