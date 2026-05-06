package com.firstpenguin.app.domain.diary.repository

import com.firstpenguin.app.domain.diary.repository.table.DiaryTagTable
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class DiaryTagRepository(
    private val dsl: DSLContext,
) {
    fun createAll(
        diaryId: Long,
        tagIds: List<Long>,
    ) {
        if (tagIds.isEmpty()) return

        val rows = tagIds.distinct().map { tagId -> DSL.row(diaryId, tagId) }
        val insertStep =
            dsl.insertInto(
                DiaryTagTable.DIARY_TAGS,
                DiaryTagTable.DIARY_ID,
                DiaryTagTable.TAG_ID,
            )

        insertStep
            .valuesOfRows(rows)
            .onConflictDoNothing()
            .execute()
    }
}
