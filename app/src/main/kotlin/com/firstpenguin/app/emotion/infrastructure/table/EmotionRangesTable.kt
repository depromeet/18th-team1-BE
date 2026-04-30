package com.firstpenguin.app.emotion.infrastructure.table

import org.jooq.impl.DSL
import java.time.LocalDateTime
import kotlin.jvm.java

internal object EmotionRangeTable {
    val EMOTION_RANGES = DSL.table(DSL.name("emotion_ranges"))
    val ID = DSL.field(DSL.name("emotion_ranges", "id"), Long::class.java)
    val NAME = DSL.field(DSL.name("emotion_ranges", "name"), String::class.java)
    val MIN_VALUE = DSL.field(DSL.name("emotion_ranges", "min_value"), Int::class.java)
    val MAX_VALUE = DSL.field(DSL.name("emotion_ranges", "max_value"), Int::class.java)
    val CREATED_AT = DSL.field(DSL.name("emotion_ranges", "created_at"), LocalDateTime::class.java)
}