package com.firstpenguin.app.domain.quotebatch.repository.table

import org.jooq.impl.DSL
import java.time.LocalDateTime

internal object QuoteBatchItemTable {
    val QUOTE_BATCH_ITEMS = DSL.table(DSL.name("quote_batch_items"))
    val ID = DSL.field(DSL.name("quote_batch_items", "id"), Long::class.java)
    val JOB_ID = DSL.field(DSL.name("quote_batch_items", "job_id"), Long::class.java)
    val JOB_TYPE = DSL.field(DSL.name("quote_batch_items", "job_type"), String::class.java)
    val TARGET_ID = DSL.field(DSL.name("quote_batch_items", "target_id"), Long::class.java)
    val CUSTOM_ID = DSL.field(DSL.name("quote_batch_items", "custom_id"), String::class.java)
    val STATUS = DSL.field(DSL.name("quote_batch_items", "status"), String::class.java)
    val ERROR_MESSAGE = DSL.field(DSL.name("quote_batch_items", "error_message"), String::class.java)
    val CREATED_AT = DSL.field(DSL.name("quote_batch_items", "created_at"), LocalDateTime::class.java)
    val UPDATED_AT = DSL.field(DSL.name("quote_batch_items", "updated_at"), LocalDateTime::class.java)
}
