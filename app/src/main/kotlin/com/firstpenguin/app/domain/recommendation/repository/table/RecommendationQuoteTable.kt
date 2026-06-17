package com.firstpenguin.app.domain.recommendation.repository.table

import org.jooq.impl.DSL
import java.time.LocalDateTime

internal object RecommendationQuoteTable {
    val RECOMMENDATION_QUOTES = DSL.table(DSL.name("recommendation_quotes"))
    val ID = DSL.field(DSL.name("recommendation_quotes", "id"), Long::class.java)
    val RECOMMENDATION_ID =
        DSL.field(DSL.name("recommendation_quotes", "recommendation_id"), Long::class.java)
    val QUOTE_ID = DSL.field(DSL.name("recommendation_quotes", "quote_id"), Long::class.java)
    val DISPLAY_ORDER = DSL.field(DSL.name("recommendation_quotes", "display_order"), Int::class.java)
    val CREATED_AT = DSL.field(DSL.name("recommendation_quotes", "created_at"), LocalDateTime::class.java)
}
