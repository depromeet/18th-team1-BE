package com.firstpenguin.app.domain.emotion.repository

import com.firstpenguin.app.domain.emotion.infrastructure.table.TagTable
import com.firstpenguin.app.domain.emotion.model.Tag
import com.firstpenguin.app.global.enums.TagType
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.springframework.stereotype.Repository

@Repository
class TagRepository(
    private val dsl: DSLContext,
) {
    fun getEmotionTagsByEmotionRangeId(emotionRangeId: Long): List<Tag> =
        dsl
            .select(tagFields())
            .from(TagTable.TAGS)
            .where(
                TagTable.TYPE
                    .eq(TagType.EMOTION.name)
                    .and(TagTable.EMOTION_RANGE_ID.eq(emotionRangeId))
                    .and(TagTable.EMOTION_RANGE_ID.isNotNull),
            ).fetch(::toTag)

    fun getToneTags(): List<Tag> =
        dsl
            .select(tagFields())
            .from(TagTable.TAGS)
            .where(
                TagTable.TYPE
                        .eq(TagType.TONE.name)
                        .and(TagTable.EMOTION_RANGE_ID.isNull),
            )
            .fetch(::toTag)

    fun getEmotionTagsByTagIdsIn(tagIds: List<Long>): List<Tag> {
        if (tagIds.isEmpty()) return emptyList()

        return dsl
            .select(tagFields())
            .from(TagTable.TAGS)
            .where(
                TagTable.ID
                    .`in`(tagIds)
                    .and(TagTable.TYPE.eq(TagType.EMOTION.name))
                    .and(TagTable.EMOTION_RANGE_ID.isNotNull),
            ).fetch(::toTag)
    }

    fun getToneTagsByTagIdsIn(tagIds: List<Long>): List<Tag> {
        if (tagIds.isEmpty()) return emptyList()

        return dsl
            .select(tagFields())
            .from(TagTable.TAGS)
            .where(
                TagTable.ID
                    .`in`(tagIds)
                    .and(TagTable.TYPE.eq(TagType.TONE.name))
                    .and(TagTable.EMOTION_RANGE_ID.isNull),
            ).fetch(::toTag)
    }

    private fun toTag(record: Record): Tag =
        Tag(
            id = record[TagTable.ID]!!,
            emotionRangeId = record[TagTable.EMOTION_RANGE_ID],
            label = record[TagTable.LABEL]!!,
            type = TagType.from(record[TagTable.TYPE]!!),
            createdAt = record[TagTable.CREATED_AT]!!,
        )

    private fun tagFields(): List<Field<*>> =
        listOf(
            TagTable.ID,
            TagTable.EMOTION_RANGE_ID,
            TagTable.LABEL,
            TagTable.TYPE,
            TagTable.CREATED_AT,
        )
}
