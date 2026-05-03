package com.firstpenguin.app.domain.recommendation.repository.table

import org.jooq.impl.DSL
import java.time.LocalDate
import java.time.LocalDateTime

internal object DailyRecommendationTable {
    val DAILY_RECOMMENDATIONS = DSL.table(DSL.name("daily_recommendations"))
    val ID = DSL.field(DSL.name("daily_recommendations", "id"), Long::class.java)
    val USER_ID = DSL.field(DSL.name("daily_recommendations", "user_id"), Long::class.java)
    val QUOTE_ID = DSL.field(DSL.name("daily_recommendations", "quote_id"), Long::class.java)
    val RECOMMENDATION_DATE =
        DSL.field(DSL.name("daily_recommendations", "recommendation_date"), LocalDate::class.java)
    val USER_CONTEXT = DSL.field(DSL.name("daily_recommendations", "user_context"), String::class.java)
    val SELECTED_EMOTION_RANGE_ID =
        DSL.field(DSL.name("daily_recommendations", "selected_emotion_range_id"), Long::class.java)
    val CREATED_AT = DSL.field(DSL.name("daily_recommendations", "created_at"), LocalDateTime::class.java)
}
