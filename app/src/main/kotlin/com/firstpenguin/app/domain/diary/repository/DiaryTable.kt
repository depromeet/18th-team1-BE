package com.firstpenguin.app.domain.diary.repository

import org.jooq.impl.DSL
import java.time.LocalDateTime

internal object DiaryTable {
    val DIARIES = DSL.table(DSL.name("diaries"))
    val ID = DSL.field(DSL.name("diaries", "id"), Long::class.java)
    val USER_ID = DSL.field(DSL.name("diaries", "user_id"), Long::class.java)
    val EMOTION_INTENSITY = DSL.field(DSL.name("diaries", "emotion_intensity"), String::class.java)
    val CONTENT = DSL.field(DSL.name("diaries", "content"), String::class.java)
    val CREATED_AT = DSL.field(DSL.name("diaries", "created_at"), LocalDateTime::class.java)
    val UPDATED_AT = DSL.field(DSL.name("diaries", "updated_at"), LocalDateTime::class.java)
    val DELETED_AT = DSL.field(DSL.name("diaries", "deleted_at"), LocalDateTime::class.java)
}
