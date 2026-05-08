ALTER TABLE books
    ADD COLUMN IF NOT EXISTS cover_image_url TEXT NOT NULL DEFAULT 'https://cdn.example.com/book-cover-placeholder.png';
