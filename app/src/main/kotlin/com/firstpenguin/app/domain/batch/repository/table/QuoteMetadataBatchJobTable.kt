package com.firstpenguin.app.domain.batch.repository.table

import org.jooq.impl.DSL
import java.time.LocalDateTime

internal object QuoteMetadataBatchJobTable {
    val QUOTE_METADATA_BATCH_JOBS = DSL.table(DSL.name("quote_metadata_batch_jobs"))
    val ID = DSL.field(DSL.name("quote_metadata_batch_jobs", "id"), Long::class.java)
    val OPENAI_BATCH_ID = DSL.field(DSL.name("quote_metadata_batch_jobs", "openai_batch_id"), String::class.java)
    val INPUT_FILE_ID = DSL.field(DSL.name("quote_metadata_batch_jobs", "input_file_id"), String::class.java)
    val OUTPUT_FILE_ID = DSL.field(DSL.name("quote_metadata_batch_jobs", "output_file_id"), String::class.java)
    val ERROR_FILE_ID = DSL.field(DSL.name("quote_metadata_batch_jobs", "error_file_id"), String::class.java)
    val STATUS = DSL.field(DSL.name("quote_metadata_batch_jobs", "status"), String::class.java)
    val METADATA_MODEL = DSL.field(DSL.name("quote_metadata_batch_jobs", "metadata_model"), String::class.java)
    val METADATA_VERSION = DSL.field(DSL.name("quote_metadata_batch_jobs", "metadata_version"), Int::class.java)
    val SUBMITTED_COUNT = DSL.field(DSL.name("quote_metadata_batch_jobs", "submitted_count"), Int::class.java)
    val SUCCEEDED_COUNT = DSL.field(DSL.name("quote_metadata_batch_jobs", "succeeded_count"), Int::class.java)
    val FAILED_COUNT = DSL.field(DSL.name("quote_metadata_batch_jobs", "failed_count"), Int::class.java)
    val CREATED_AT = DSL.field(DSL.name("quote_metadata_batch_jobs", "created_at"), LocalDateTime::class.java)
    val UPDATED_AT = DSL.field(DSL.name("quote_metadata_batch_jobs", "updated_at"), LocalDateTime::class.java)
    val COMPLETED_AT = DSL.field(DSL.name("quote_metadata_batch_jobs", "completed_at"), LocalDateTime::class.java)
}
