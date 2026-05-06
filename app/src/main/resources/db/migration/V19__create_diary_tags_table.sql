CREATE TABLE diary_tags (
    diary_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    UNIQUE (diary_id, tag_id)
);