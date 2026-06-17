CREATE TABLE IF NOT EXISTS monthly_settlements (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    settlement_year INT NOT NULL,
    settlement_month INT NOT NULL,
    shared_quote_count INT NOT NULL,
    most_frequent_genre VARCHAR(100),
    top_emotion_tag_id BIGINT NOT NULL,
    top_emotion_tag_label VARCHAR(100) NOT NULL,
    recommendation_message TEXT NOT NULL,
    selected_quote_id BIGINT,
    selected_quote_content TEXT,
    selected_book_id BIGINT,
    selected_book_title VARCHAR(255),
    selected_book_author VARCHAR(255),
    selected_book_cover_image_url TEXT,
    selected_book_genre VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT monthly_settlements_month_check CHECK (settlement_month BETWEEN 1 AND 12),
    CONSTRAINT monthly_settlements_shared_quote_count_check CHECK (shared_quote_count > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS monthly_settlements_user_month_uidx
    ON monthly_settlements (user_id, settlement_year, settlement_month);

CREATE INDEX IF NOT EXISTS monthly_settlements_user_id_idx
    ON monthly_settlements (user_id);

CREATE TABLE IF NOT EXISTS monthly_settlement_emotion_tags (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    monthly_settlement_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    tag_label VARCHAR(100) NOT NULL,
    tag_count INT NOT NULL,
    sort_order INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT monthly_settlement_emotion_tags_count_check CHECK (tag_count > 0),
    CONSTRAINT monthly_settlement_emotion_tags_sort_order_check CHECK (sort_order BETWEEN 1 AND 10)
);

CREATE UNIQUE INDEX IF NOT EXISTS monthly_settlement_emotion_tags_settlement_tag_uidx
    ON monthly_settlement_emotion_tags (monthly_settlement_id, tag_id);

CREATE UNIQUE INDEX IF NOT EXISTS monthly_settlement_emotion_tags_settlement_sort_uidx
    ON monthly_settlement_emotion_tags (monthly_settlement_id, sort_order);

CREATE TABLE IF NOT EXISTS monthly_settlement_books (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    monthly_settlement_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    book_cover_image_url TEXT NOT NULL,
    genre VARCHAR(100) NOT NULL,
    sort_order INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT monthly_settlement_books_sort_order_check CHECK (sort_order BETWEEN 1 AND 3)
);

CREATE UNIQUE INDEX IF NOT EXISTS monthly_settlement_books_settlement_book_uidx
    ON monthly_settlement_books (monthly_settlement_id, book_id);

CREATE UNIQUE INDEX IF NOT EXISTS monthly_settlement_books_settlement_sort_uidx
    ON monthly_settlement_books (monthly_settlement_id, sort_order);
