package com.firstpenguin.app.domain.quotecreation.review.repository.table

import org.jooq.impl.DSL
import java.time.LocalDateTime

internal object QuoteCandidateTable {
    val QUOTE_CANDIDATES = DSL.table(DSL.name("quote_candidates"))
    val ID = DSL.field(DSL.name("quote_candidates", "id"), Long::class.java)
    val BOOK_ID = DSL.field(DSL.name("quote_candidates", "book_id"), Long::class.java)
    val CONTENT = DSL.field(DSL.name("quote_candidates", "content"), String::class.java)
    val SOURCE = DSL.field(DSL.name("quote_candidates", "source"), String::class.java)
    val STATUS = DSL.field(DSL.name("quote_candidates", "status"), String::class.java)
    val REJECT_REASON = DSL.field(DSL.name("quote_candidates", "reject_reason"), String::class.java)
    val CREATED_AT = DSL.field(DSL.name("quote_candidates", "created_at"), LocalDateTime::class.java)
    val UPDATED_AT = DSL.field(DSL.name("quote_candidates", "updated_at"), LocalDateTime::class.java)
}
