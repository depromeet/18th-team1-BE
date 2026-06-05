package com.firstpenguin.app.domain.quotemetadata.repository.table

import org.jooq.impl.DSL
import java.time.LocalDateTime

internal object QuoteMetadataBatchItemTable {
    val QUOTE_METADATA_BATCH_ITEMS = DSL.table(DSL.name("quote_metadata_batch_items"))
    val ID = DSL.field(DSL.name("quote_metadata_batch_items", "id"), Long::class.java)
    val JOB_ID = DSL.field(DSL.name("quote_metadata_batch_items", "job_id"), Long::class.java)
    val QUOTE_ID = DSL.field(DSL.name("quote_metadata_batch_items", "quote_id"), Long::class.java)
    val STATUS = DSL.field(DSL.name("quote_metadata_batch_items", "status"), String::class.java)
    val ERROR_MESSAGE = DSL.field(DSL.name("quote_metadata_batch_items", "error_message"), String::class.java)
    val CREATED_AT = DSL.field(DSL.name("quote_metadata_batch_items", "created_at"), LocalDateTime::class.java)
    val UPDATED_AT = DSL.field(DSL.name("quote_metadata_batch_items", "updated_at"), LocalDateTime::class.java)
}
