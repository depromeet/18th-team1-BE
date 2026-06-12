DROP TABLE IF EXISTS quote_extraction_batch_item_quotes;
DROP TABLE IF EXISTS quote_extraction_batch_items;
DROP TABLE IF EXISTS quote_extraction_batch_jobs;
DROP TABLE IF EXISTS quote_metadata_batch_items;
DROP TABLE IF EXISTS quote_metadata_batch_jobs;
DROP TABLE IF EXISTS quote_extraction_candidates;

CREATE TABLE IF NOT EXISTS quote_batch_jobs (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    job_type VARCHAR(30) NOT NULL,
    openai_batch_id VARCHAR(100),
    input_file_id VARCHAR(100),
    output_file_id VARCHAR(100),
    error_file_id VARCHAR(100),
    status VARCHAR(30) NOT NULL,
    model VARCHAR(100) NOT NULL,
    version INT NOT NULL,
    submitted_count INT NOT NULL DEFAULT 0,
    succeeded_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT chk_quote_batch_job_job_type
    CHECK (job_type IN (
           'QUOTE_METADATA',
           'QUOTE_EXTRACTION',
           'QUOTE_REVIEW'
                       )),
    CONSTRAINT chk_quote_batch_job_status
    CHECK (status IN (
           'PREPARING',
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
    CONSTRAINT chk_quote_batch_job_counts
    CHECK (
              submitted_count >= 0
              AND succeeded_count >= 0
              AND failed_count >= 0
              AND succeeded_count + failed_count <= submitted_count
          )
    );

CREATE UNIQUE INDEX IF NOT EXISTS quote_batch_jobs_openai_batch_id_uidx
    ON quote_batch_jobs (openai_batch_id);

CREATE INDEX IF NOT EXISTS quote_batch_jobs_status_idx
    ON quote_batch_jobs (status);

CREATE UNIQUE INDEX IF NOT EXISTS quote_batch_jobs_single_running_uidx
    ON quote_batch_jobs ((1))
    WHERE status IN (
        'PREPARING',
        'SUBMITTED',
        'VALIDATING',
        'IN_PROGRESS',
        'FINALIZING',
        'CANCELLING'
    );

CREATE TABLE IF NOT EXISTS quote_batch_items (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    job_id BIGINT NOT NULL,
    job_type VARCHAR(30) NOT NULL,
    target_id BIGINT NOT NULL,
    custom_id VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_quote_batch_item_job_type
    CHECK (job_type IN (
           'QUOTE_METADATA',
           'QUOTE_EXTRACTION',
           'QUOTE_REVIEW'
                       )),
    CONSTRAINT chk_quote_batch_item_status
    CHECK (status IN ('PREPARING', 'SUBMITTED', 'SUCCEEDED', 'FAILED'))
    );

CREATE UNIQUE INDEX IF NOT EXISTS quote_batch_items_job_target_uidx
    ON quote_batch_items (job_id, target_id);

CREATE UNIQUE INDEX IF NOT EXISTS quote_batch_items_active_target_uidx
    ON quote_batch_items (job_type, target_id)
    WHERE status IN ('PREPARING', 'SUBMITTED');

CREATE INDEX IF NOT EXISTS quote_batch_items_job_id_idx
    ON quote_batch_items (job_id);

CREATE INDEX IF NOT EXISTS quote_batch_items_target_id_idx
    ON quote_batch_items (target_id);
