package com.firstpenguin.app.domain.quotemetadata.repository.table

import org.jooq.impl.DSL
import java.time.LocalDateTime

internal object QuoteMetadataTagTable {
    val QUOTE_METADATA_TAGS = DSL.table(DSL.name("quote_metadata_tags"))
    val ID = DSL.field(DSL.name("quote_metadata_tags", "id"), Long::class.java)
    val QUOTE_METADATA_ID = DSL.field(DSL.name("quote_metadata_tags", "quote_metadata_id"), Long::class.java)
    val TAG_ID = DSL.field(DSL.name("quote_metadata_tags", "tag_id"), Long::class.java)
    val CREATED_AT = DSL.field(DSL.name("quote_metadata_tags", "created_at"), LocalDateTime::class.java)
}
