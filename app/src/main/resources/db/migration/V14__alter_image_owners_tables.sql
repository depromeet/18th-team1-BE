ALTER TABLE image_owners
    ADD CONSTRAINT chk_image_owner_type
        CHECK (owner_type IN ('USER', 'DIARY', 'BOOK', 'REPORT'));
