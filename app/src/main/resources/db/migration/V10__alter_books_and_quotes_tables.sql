ALTER SEQUENCE books_id_seq INCREMENT BY 1;
ALTER SEQUENCE quotes_id_seq INCREMENT BY 1;

ALTER TABLE books
    DROP COLUMN cover_image_url;

ALTER TABLE books
    ADD COLUMN publisher VARCHAR(100);
