package com.firstpenguin.app.domain.quote.repository

import org.jooq.impl.DSL
import java.time.LocalDateTime

internal object QuoteScrapTable {
    val QUOTE_SCRAPS = DSL.table(DSL.name("quote_scraps"))
    val ID = DSL.field(DSL.name("quote_scraps", "id"), Long::class.java)
    val USER_ID = DSL.field(DSL.name("quote_scraps", "user_id"), Long::class.java)
    val QUOTE_ID = DSL.field(DSL.name("quote_scraps", "quote_id"), Long::class.java)
    val CREATED_AT = DSL.field(DSL.name("quote_scraps", "created_at"), LocalDateTime::class.java)
}
