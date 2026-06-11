ALTER TABLE books
    ADD COLUMN IF NOT EXISTS category VARCHAR(50);

CREATE INDEX IF NOT EXISTS books_category_idx
    ON books (category);

UPDATE books
SET category = '고전문학'
WHERE category IS NULL
  AND title IN ('데미안', '어린 왕자');

UPDATE books
SET category = '한국소설'
WHERE category IS NULL
  AND title = '모순';
