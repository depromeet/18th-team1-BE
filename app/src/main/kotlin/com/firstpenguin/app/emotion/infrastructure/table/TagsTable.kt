package com.firstpenguin.app.emotion.infrastructure.table

import org.jooq.impl.DSL
import java.time.LocalDateTime

internal object TagTable {
    val TAGS = DSL.table(DSL.name("tags"))
    val ID = DSL.field(DSL.name("tags", "id"), Long::class.java)
    val EMOTION_RANGE_ID = DSL.field(DSL.name("tags", "emotion_range_id"), Long::class.java)
    val LABEL = DSL.field(DSL.name("tags", "label"), String::class.java)
    val CREATED_AT = DSL.field(DSL.name("tags", "created_at"), LocalDateTime::class.java)
}