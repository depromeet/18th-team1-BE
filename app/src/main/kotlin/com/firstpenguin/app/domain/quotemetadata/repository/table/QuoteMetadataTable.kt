package com.firstpenguin.app.domain.quotemetadata.repository.table

import org.jooq.impl.DSL
import java.time.LocalDateTime

internal object QuoteMetadataTable {
    val QUOTE_METADATA = DSL.table(DSL.name("quote_metadata"))
    val ID = DSL.field(DSL.name("quote_metadata", "id"), Long::class.java)
    val QUOTE_ID = DSL.field(DSL.name("quote_metadata", "quote_id"), Long::class.java)
    val EMBEDDING_TEXT = DSL.field(DSL.name("quote_metadata", "embedding_text"), String::class.java)
    val METADATA_MODEL = DSL.field(DSL.name("quote_metadata", "metadata_model"), String::class.java)
    val METADATA_VERSION = DSL.field(DSL.name("quote_metadata", "metadata_version"), Int::class.java)
    val CREATED_AT = DSL.field(DSL.name("quote_metadata", "created_at"), LocalDateTime::class.java)
    val UPDATED_AT = DSL.field(DSL.name("quote_metadata", "updated_at"), LocalDateTime::class.java)
}
