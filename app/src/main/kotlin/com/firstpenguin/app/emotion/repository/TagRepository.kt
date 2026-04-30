package com.firstpenguin.app.emotion.repository

import com.firstpenguin.app.emotion.infrastructure.table.TagTable
import com.firstpenguin.app.emotion.model.Tag
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.springframework.stereotype.Repository

@Repository
class TagRepository(
    private val dsl: DSLContext,
) {
    fun getTagList(emotionRangeId: Long): List<Tag> =
        dsl
            .select(TAG_FIELDS)
            .from(TagTable.TAGS)
            .where(TagTable.EMOTION_RANGE_ID.eq(emotionRangeId))
            .fetch(::toTag)

    private fun toTag(record: Record): Tag =
        Tag(
            id = record.get(TagTable.ID),
            emotionRangeId = record.get(TagTable.EMOTION_RANGE_ID),
            label = record.get(TagTable.LABEL)
        )

    private companion object {
        val TAG_FIELDS: List<Field<*>> =
            listOf(
                TagTable.ID,
                TagTable.EMOTION_RANGE_ID,
                TagTable.LABEL,
                TagTable.CREATED_AT
            )
    }
}