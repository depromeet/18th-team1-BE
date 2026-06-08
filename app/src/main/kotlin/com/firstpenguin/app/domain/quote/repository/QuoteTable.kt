package com.firstpenguin.app.domain.quote.repository

import org.jooq.impl.DSL
import java.time.LocalDateTime

internal object QuoteTable {
    val QUOTES = DSL.table(DSL.name("quotes"))
    val ID = DSL.field(DSL.name("quotes", "id"), Long::class.java)
    val BOOK_ID = DSL.field(DSL.name("quotes", "book_id"), Long::class.java)
    val CONTENT = DSL.field(DSL.name("quotes", "content"), String::class.java)
    val SOURCE_TYPE = DSL.field(DSL.name("quotes", "source_type"), String::class.java)
    val CREATED_AT = DSL.field(DSL.name("quotes", "created_at"), LocalDateTime::class.java)
    val UPDATED_AT = DSL.field(DSL.name("quotes", "updated_at"), LocalDateTime::class.java)
    val DELETED_AT = DSL.field(DSL.name("quotes", "deleted_at"), LocalDateTime::class.java)
}
