package com.firstpenguin.app.domain.monthlysettlement.repository.table

import org.jooq.impl.DSL
import java.time.LocalDateTime

internal object MonthlySettlementBookTable {
    val MONTHLY_SETTLEMENT_BOOKS = DSL.table(DSL.name("monthly_settlement_books"))
    val ID = DSL.field(DSL.name("monthly_settlement_books", "id"), Long::class.java)
    val MONTHLY_SETTLEMENT_ID =
        DSL.field(DSL.name("monthly_settlement_books", "monthly_settlement_id"), Long::class.java)
    val BOOK_ID = DSL.field(DSL.name("monthly_settlement_books", "book_id"), Long::class.java)
    val TITLE = DSL.field(DSL.name("monthly_settlement_books", "title"), String::class.java)
    val AUTHOR = DSL.field(DSL.name("monthly_settlement_books", "author"), String::class.java)
    val BOOK_COVER_IMAGE_URL =
        DSL.field(DSL.name("monthly_settlement_books", "book_cover_image_url"), String::class.java)
    val GENRE = DSL.field(DSL.name("monthly_settlement_books", "genre"), String::class.java)
    val SORT_ORDER = DSL.field(DSL.name("monthly_settlement_books", "sort_order"), Int::class.java)
    val CREATED_AT = DSL.field(DSL.name("monthly_settlement_books", "created_at"), LocalDateTime::class.java)
}
