package com.firstpenguin.app.domain.recommendation.repository.table

import org.jooq.impl.DSL
import java.time.LocalDateTime

internal object DailyRecommendationTagTable {
    val DAILY_RECOMMENDATION_TAGS = DSL.table(DSL.name("daily_recommendation_tags"))
    val ID = DSL.field(DSL.name("daily_recommendation_tags", "id"), Long::class.java)
    val DAILY_RECOMMENDATION_ID =
        DSL.field(DSL.name("daily_recommendation_tags", "daily_recommendation_id"), Long::class.java)
    val TAG_ID = DSL.field(DSL.name("daily_recommendation_tags", "tag_id"), Long::class.java)
    val CREATED_AT = DSL.field(DSL.name("daily_recommendation_tags", "created_at"), LocalDateTime::class.java)
}
