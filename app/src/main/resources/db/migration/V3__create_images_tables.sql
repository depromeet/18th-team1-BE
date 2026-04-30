CREATE SEQUENCE images_id_seq
    START WITH 1
    INCREMENT BY 50;

CREATE TABLE images (
    id BIGINT PRIMARY KEY DEFAULT nextval('images_id_seq'),
    url TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE image_owners (
    image_id BIGINT NOT NULL,
    owner_type VARCHAR(50) NOT NULL,
    owner_id BIGINT NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT image_owners_sort_order_check CHECK (sort_order >= 0)
);

CREATE UNIQUE INDEX image_owners_owner_image_unique
    ON image_owners (owner_type, owner_id, image_id);

CREATE UNIQUE INDEX image_owners_owner_sort_order_unique
    ON image_owners (owner_type, owner_id, sort_order);

CREATE INDEX image_owners_owner_idx
    ON image_owners (owner_type, owner_id, sort_order);

CREATE INDEX image_owners_image_id_idx
    ON image_owners (image_id);
