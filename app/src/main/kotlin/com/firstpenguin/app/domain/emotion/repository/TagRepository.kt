package com.firstpenguin.app.domain.emotion.repository

import com.firstpenguin.app.domain.batch.dto.TagOption
import com.firstpenguin.app.domain.emotion.model.Tag
import com.firstpenguin.app.domain.emotion.repository.table.TagTable
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
                    .and(TagTable.EMOTION_RANGE_ID.isNotNull)
                    .and(TagTable.IS_ACTIVE.isTrue),
            ).orderBy(TagTable.SORT_ORDER.asc())
            .fetch(::toTag)

    fun getNeedTags(): List<Tag> =
        dsl
            .select(tagFields())
            .from(TagTable.TAGS)
            .where(
                TagTable.TYPE
                    .eq(TagType.NEED.name)
                    .and(TagTable.EMOTION_RANGE_ID.isNull)
                    .and(TagTable.IS_ACTIVE.isTrue),
            ).orderBy(TagTable.SORT_ORDER.asc())
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
                    .and(TagTable.EMOTION_RANGE_ID.isNotNull)
                    .and(TagTable.IS_ACTIVE.isTrue),
            ).fetch(::toTag)
    }

    fun getNeedTagsByTagIdsIn(tagIds: List<Long>): List<Tag> {
        if (tagIds.isEmpty()) return emptyList()

        return dsl
            .select(tagFields())
            .from(TagTable.TAGS)
            .where(
                TagTable.ID
                    .`in`(tagIds)
                    .and(TagTable.TYPE.eq(TagType.NEED.name))
                    .and(TagTable.EMOTION_RANGE_ID.isNull)
                    .and(TagTable.IS_ACTIVE.isTrue),
            ).fetch(::toTag)
    }

    fun getActiveTagsByType(): Map<TagType, List<TagOption>> =
        dsl
            .select(
                TagTable.ID,
                TagTable.TYPE,
                TagTable.CODE,
                TagTable.LABEL,
                TagTable.DESCRIPTION,
            ).from(TagTable.TAGS)
            .where(TagTable.IS_ACTIVE.isTrue)
            .orderBy(TagTable.TYPE.asc(), TagTable.SORT_ORDER.asc(), TagTable.ID.asc())
            .fetch(::toTagOption)
            .groupBy { tag -> tag.type }

    private fun toTag(record: Record): Tag =
        Tag(
            id = record[TagTable.ID]!!,
            emotionRangeId = record[TagTable.EMOTION_RANGE_ID],
            label = record[TagTable.LABEL]!!,
            type = TagType.from(record[TagTable.TYPE]!!),
            createdAt = record[TagTable.CREATED_AT]!!,
        )

    private fun toTagOption(record: Record): TagOption =
        TagOption(
            id = record[TagTable.ID]!!,
            type = TagType.from(record[TagTable.TYPE]!!),
            code = record[TagTable.CODE]!!,
            label = record[TagTable.LABEL]!!,
            description = record[TagTable.DESCRIPTION],
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
