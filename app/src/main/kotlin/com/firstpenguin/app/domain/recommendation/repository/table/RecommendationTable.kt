package com.firstpenguin.app.domain.recommendation.repository.table

import org.jooq.impl.DSL
import java.time.LocalDate
import java.time.LocalDateTime

internal object RecommendationTable {
    val RECOMMENDATIONS = DSL.table(DSL.name("recommendations"))
    val ID = DSL.field(DSL.name("recommendations", "id"), Long::class.java)
    val USER_ID = DSL.field(DSL.name("recommendations", "user_id"), Long::class.java)
    val QUOTE_ID = DSL.field(DSL.name("recommendations", "quote_id"), Long::class.java)
    val RECOMMENDATION_DATE =
        DSL.field(DSL.name("recommendations", "recommendation_date"), LocalDate::class.java)
    val FEELING_TEXT = DSL.field(DSL.name("recommendations", "feeling_text"), String::class.java)
    val DIARY_TEXT = DSL.field(DSL.name("recommendations", "diary_text"), String::class.java)
    val EMOTION_VALUE =
        DSL.field(DSL.name("recommendations", "emotion_value"), Int::class.java)
    val EMOTION_RANGE_ID =
        DSL.field(DSL.name("recommendations", "emotion_range_id"), Long::class.java)
    val CREATED_AT = DSL.field(DSL.name("recommendations", "created_at"), LocalDateTime::class.java)
}
