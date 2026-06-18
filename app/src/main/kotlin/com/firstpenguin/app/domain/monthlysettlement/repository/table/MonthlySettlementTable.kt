package com.firstpenguin.app.domain.monthlysettlement.repository.table

import org.jooq.impl.DSL
import java.time.LocalDateTime

internal object MonthlySettlementTable {
    val MONTHLY_SETTLEMENTS = DSL.table(DSL.name("monthly_settlements"))
    val ID = DSL.field(DSL.name("monthly_settlements", "id"), Long::class.java)
    val USER_ID = DSL.field(DSL.name("monthly_settlements", "user_id"), Long::class.java)
    val SETTLEMENT_YEAR = DSL.field(DSL.name("monthly_settlements", "settlement_year"), Int::class.java)
    val SETTLEMENT_MONTH = DSL.field(DSL.name("monthly_settlements", "settlement_month"), Int::class.java)
    val SHARED_QUOTE_COUNT = DSL.field(DSL.name("monthly_settlements", "shared_quote_count"), Int::class.java)
    val MOST_FREQUENT_GENRE = DSL.field(DSL.name("monthly_settlements", "most_frequent_genre"), String::class.java)
    val TOP_EMOTION_TAG_ID = DSL.field(DSL.name("monthly_settlements", "top_emotion_tag_id"), Long::class.java)
    val TOP_EMOTION_TAG_LABEL =
        DSL.field(DSL.name("monthly_settlements", "top_emotion_tag_label"), String::class.java)
    val RECOMMENDATION_MESSAGE =
        DSL.field(DSL.name("monthly_settlements", "recommendation_message"), String::class.java)
    val SELECTED_QUOTE_ID = DSL.field(DSL.name("monthly_settlements", "selected_quote_id"), Long::class.java)
    val SELECTED_QUOTE_CONTENT =
        DSL.field(DSL.name("monthly_settlements", "selected_quote_content"), String::class.java)
    val SELECTED_BOOK_ID = DSL.field(DSL.name("monthly_settlements", "selected_book_id"), Long::class.java)
    val SELECTED_BOOK_TITLE =
        DSL.field(DSL.name("monthly_settlements", "selected_book_title"), String::class.java)
    val SELECTED_BOOK_AUTHOR =
        DSL.field(DSL.name("monthly_settlements", "selected_book_author"), String::class.java)
    val SELECTED_BOOK_COVER_IMAGE_URL =
        DSL.field(DSL.name("monthly_settlements", "selected_book_cover_image_url"), String::class.java)
    val SELECTED_BOOK_GENRE =
        DSL.field(DSL.name("monthly_settlements", "selected_book_genre"), String::class.java)
    val SELECTED_BOOK_PURCHASE_LINK =
        DSL.field(DSL.name("monthly_settlements", "selected_book_purchase_link"), String::class.java)
    val CREATED_AT = DSL.field(DSL.name("monthly_settlements", "created_at"), LocalDateTime::class.java)
}
