package com.firstpenguin.app.domain.diary.repository.table

import org.jooq.impl.DSL

internal object DiaryTagTable {
    val DIARY_TAGS = DSL.table(DSL.name("diary_tags"))
    val DIARY_ID = DSL.field(DSL.name("diary_tags", "diary_id"), Long::class.java)
    val TAG_ID = DSL.field(DSL.name("diary_tags", "tag_id"), Long::class.java)
}
