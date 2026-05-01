CREATE SEQUENCE diaries_id_seq
    START WITH 1
    INCREMENT BY 50;

CREATE TABLE diaries (
    id BIGINT PRIMARY KEY DEFAULT nextval('diaries_id_seq'),
    user_id BIGINT NOT NULL,
    quote_id BIGINT NOT NULL,
    emotion_intensity VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX diaries_user_id_idx ON diaries (user_id);

CREATE INDEX diaries_quote_id_idx ON diaries (quote_id);

CREATE INDEX diaries_deleted_at_idx ON diaries (deleted_at);

CREATE SEQUENCE books_id_seq
    START WITH 1
    INCREMENT BY 50;

CREATE TABLE books (
    id BIGINT PRIMARY KEY DEFAULT nextval('books_id_seq'),
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    isbn13 VARCHAR(13) NOT NULL,
    aladin_link TEXT NOT NULL,
    cover_image_url TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX books_isbn13_idx ON books (isbn13);

CREATE INDEX books_deleted_at_idx ON books (deleted_at);

CREATE SEQUENCE quotes_id_seq
    START WITH 1
    INCREMENT BY 50;

CREATE TABLE quotes (
    id BIGINT PRIMARY KEY DEFAULT nextval('quotes_id_seq'),
    book_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX quotes_book_id_idx ON quotes (book_id);

CREATE INDEX quotes_deleted_at_idx ON quotes (deleted_at);
