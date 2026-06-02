ALTER TABLE quote_metadata_batch_jobs
    ALTER COLUMN openai_batch_id DROP NOT NULL,
    ALTER COLUMN input_file_id DROP NOT NULL;

ALTER TABLE quote_metadata_batch_jobs
    DROP CONSTRAINT chk_quote_metadata_batch_job_status;

ALTER TABLE quote_metadata_batch_jobs
    ADD CONSTRAINT chk_quote_metadata_batch_job_status
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
        ));

CREATE UNIQUE INDEX IF NOT EXISTS quote_metadata_batch_jobs_single_running_uidx
    ON quote_metadata_batch_jobs ((1))
    WHERE status IN (
        'PREPARING',
        'SUBMITTED',
        'VALIDATING',
        'IN_PROGRESS',
        'FINALIZING',
        'CANCELLING'
    );

ALTER TABLE quote_metadata_batch_items
    DROP CONSTRAINT chk_quote_metadata_batch_item_status;

ALTER TABLE quote_metadata_batch_items
    ADD CONSTRAINT chk_quote_metadata_batch_item_status
        CHECK (status IN ('PREPARING', 'SUBMITTED', 'SUCCEEDED', 'FAILED'));

DROP INDEX quote_metadata_batch_items_active_quote_uidx;

CREATE UNIQUE INDEX IF NOT EXISTS quote_metadata_batch_items_active_quote_uidx
    ON quote_metadata_batch_items (quote_id)
    WHERE status IN ('PREPARING', 'SUBMITTED');
