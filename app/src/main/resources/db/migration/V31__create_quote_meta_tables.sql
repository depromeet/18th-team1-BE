CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE quote_metadata (
                                id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                quote_id BIGINT NOT NULL,
                                embedding_text TEXT NOT NULL,
                                metadata_model VARCHAR(100) NOT NULL,
                                metadata_version INT NOT NULL,
                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX quote_metadata_quote_id_uidx
    ON quote_metadata (quote_id);

CREATE TABLE quote_metadata_tags (
                                     id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                     quote_metadata_id BIGINT NOT NULL,
                                     tag_id BIGINT NOT NULL,
                                     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX quote_metadata_tags_metadata_tag_uidx
    ON quote_metadata_tags (quote_metadata_id, tag_id);

CREATE INDEX quote_metadata_tags_tag_id_idx
    ON quote_metadata_tags (tag_id);

CREATE TABLE quote_embeddings (
                                  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                  quote_id BIGINT NOT NULL,
                                  embedding_model VARCHAR(100) NOT NULL,
                                  embedding VECTOR(1536) NOT NULL,
                                  embedding_text_hash VARCHAR(64) NOT NULL,
                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX quote_embeddings_quote_model_uidx
    ON quote_embeddings (quote_id, embedding_model);
