package com.firstpenguin.app.domain.emotion.repository.table

import org.jooq.impl.DSL
import java.time.LocalDateTime
import kotlin.jvm.java

internal object TagTable {
    val TAGS = DSL.table(DSL.name("tags"))
    val ID = DSL.field(DSL.name("tags", "id"), Long::class.java)
    val EMOTION_RANGE_ID = DSL.field(DSL.name("tags", "emotion_range_id"), Long::class.javaObjectType)
    val LABEL = DSL.field(DSL.name("tags", "label"), String::class.java)
    val TYPE = DSL.field(DSL.name("tags", "type"), String::class.java)
    val CODE = DSL.field(DSL.name("tags", "code"), String::class.java)
    val SORT_ORDER = DSL.field(DSL.name("tags", "sort_order"), Int::class.java)
    val IS_ACTIVE = DSL.field(DSL.name("tags", "is_active"), Boolean::class.java)
    val CREATED_AT = DSL.field(DSL.name("tags", "created_at"), LocalDateTime::class.java)
}
