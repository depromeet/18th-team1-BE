package com.firstpenguin.app.domain.recommendation.repository.table

import org.jooq.impl.DSL
import java.time.LocalDateTime

internal object RecommendationTagTable {
    val RECOMMENDATION_TAGS = DSL.table(DSL.name("recommendation_tags"))
    val ID = DSL.field(DSL.name("recommendation_tags", "id"), Long::class.java)
    val RECOMMENDATION_ID =
        DSL.field(DSL.name("recommendation_tags", "recommendation_id"), Long::class.java)
    val TAG_ID = DSL.field(DSL.name("recommendation_tags", "tag_id"), Long::class.java)
    val CREATED_AT = DSL.field(DSL.name("recommendation_tags", "created_at"), LocalDateTime::class.java)
}
