CREATE TABLE IF NOT EXISTS quote_metadata_batch_jobs (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    openai_batch_id VARCHAR(100) NOT NULL,
    input_file_id VARCHAR(100) NOT NULL,
    output_file_id VARCHAR(100),
    error_file_id VARCHAR(100),
    status VARCHAR(30) NOT NULL,
    metadata_model VARCHAR(100) NOT NULL,
    metadata_version INT NOT NULL,
    submitted_count INT NOT NULL DEFAULT 0,
    succeeded_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT chk_quote_metadata_batch_job_status
        CHECK (status IN (
            'SUBMITTED',
            'VALIDATING',
            'IN_PROGRESS',
            'FINALIZING',
            'COMPLETED',
            'FAILED',
            'EXPIRED',
            'CANCELLING',
            'CANCELLED'
        )),
    CONSTRAINT chk_quote_metadata_batch_job_counts
        CHECK (
            submitted_count >= 0
            AND succeeded_count >= 0
            AND failed_count >= 0
            AND succeeded_count + failed_count <= submitted_count
        )
);

CREATE UNIQUE INDEX IF NOT EXISTS quote_metadata_batch_jobs_openai_batch_id_uidx
    ON quote_metadata_batch_jobs (openai_batch_id);

CREATE INDEX IF NOT EXISTS quote_metadata_batch_jobs_status_idx
    ON quote_metadata_batch_jobs (status);

CREATE TABLE IF NOT EXISTS quote_metadata_batch_items (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    job_id BIGINT NOT NULL,
    quote_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_quote_metadata_batch_item_status
        CHECK (status IN ('SUBMITTED', 'SUCCEEDED', 'FAILED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS quote_metadata_batch_items_job_quote_uidx
    ON quote_metadata_batch_items (job_id, quote_id);

CREATE UNIQUE INDEX IF NOT EXISTS quote_metadata_batch_items_active_quote_uidx
    ON quote_metadata_batch_items (quote_id)
    WHERE status = 'SUBMITTED';

CREATE INDEX IF NOT EXISTS quote_metadata_batch_items_job_id_idx
    ON quote_metadata_batch_items (job_id);

CREATE INDEX IF NOT EXISTS quote_metadata_batch_items_quote_id_idx
    ON quote_metadata_batch_items (quote_id);
