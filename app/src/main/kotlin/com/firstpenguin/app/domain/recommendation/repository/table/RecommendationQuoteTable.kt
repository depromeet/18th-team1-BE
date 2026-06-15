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
    val CANDIDATE_SOURCE = DSL.field(DSL.name("recommendation_quotes", "candidate_source"), String::class.java)
    val NEED_SCORE = DSL.field(DSL.name("recommendation_quotes", "need_score"), Double::class.java)
    val EMOTION_SCORE = DSL.field(DSL.name("recommendation_quotes", "emotion_score"), Double::class.java)
    val CONTEXT_SCORE = DSL.field(DSL.name("recommendation_quotes", "context_score"), Double::class.java)
    val SITUATION_SCORE = DSL.field(DSL.name("recommendation_quotes", "situation_score"), Double::class.java)
    val MOOD_SCORE = DSL.field(DSL.name("recommendation_quotes", "mood_score"), Double::class.java)
    val METADATA_SCORE = DSL.field(DSL.name("recommendation_quotes", "metadata_score"), Double::class.java)
    val SEMANTIC_SCORE = DSL.field(DSL.name("recommendation_quotes", "semantic_score"), Double::class.java)
    val FINAL_SCORE = DSL.field(DSL.name("recommendation_quotes", "final_score"), Double::class.java)
    val CREATED_AT = DSL.field(DSL.name("recommendation_quotes", "created_at"), LocalDateTime::class.java)
}
