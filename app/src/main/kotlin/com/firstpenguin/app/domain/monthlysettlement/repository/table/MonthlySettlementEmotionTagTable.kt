package com.firstpenguin.app.domain.monthlysettlement.repository.table

import org.jooq.impl.DSL
import java.time.LocalDateTime

internal object MonthlySettlementEmotionTagTable {
    val MONTHLY_SETTLEMENT_EMOTION_TAGS = DSL.table(DSL.name("monthly_settlement_emotion_tags"))
    val ID = DSL.field(DSL.name("monthly_settlement_emotion_tags", "id"), Long::class.java)
    val MONTHLY_SETTLEMENT_ID =
        DSL.field(DSL.name("monthly_settlement_emotion_tags", "monthly_settlement_id"), Long::class.java)
    val TAG_ID = DSL.field(DSL.name("monthly_settlement_emotion_tags", "tag_id"), Long::class.java)
    val TAG_LABEL = DSL.field(DSL.name("monthly_settlement_emotion_tags", "tag_label"), String::class.java)
    val TAG_COUNT = DSL.field(DSL.name("monthly_settlement_emotion_tags", "tag_count"), Int::class.java)
    val SORT_ORDER = DSL.field(DSL.name("monthly_settlement_emotion_tags", "sort_order"), Int::class.java)
    val CREATED_AT = DSL.field(DSL.name("monthly_settlement_emotion_tags", "created_at"), LocalDateTime::class.java)
}
