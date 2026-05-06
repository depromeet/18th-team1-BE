package com.firstpenguin.app.domain.emotion.repository

import com.firstpenguin.app.domain.emotion.model.EmotionRange
import com.firstpenguin.app.domain.emotion.repository.table.EmotionRangeTable
import com.firstpenguin.app.global.enums.EmotionRangeName
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.springframework.stereotype.Repository

@Repository
class EmotionRangeRepository(
    private val dsl: DSLContext,
) {
    fun getEmotionRange(value: Int): EmotionRange? =
        dsl
            .select(EMOTION_RANGE_FIELDS)
            .from(EmotionRangeTable.EMOTION_RANGES)
            .where(EmotionRangeTable.MIN_VALUE.le(value))
            .and(EmotionRangeTable.MAX_VALUE.ge(value))
            .fetchOne(::toEmotionRange)

    private fun toEmotionRange(record: Record): EmotionRange =
        EmotionRange(
            id = record.get(EmotionRangeTable.ID),
            name = EmotionRangeName.from(record.get(EmotionRangeTable.NAME)),
            minValue = record.get(EmotionRangeTable.MIN_VALUE),
            maxValue = record.get(EmotionRangeTable.MAX_VALUE),
        )

    private companion object {
        val EMOTION_RANGE_FIELDS: List<Field<*>> =
            listOf(
                EmotionRangeTable.ID,
                EmotionRangeTable.NAME,
                EmotionRangeTable.MIN_VALUE,
                EmotionRangeTable.MAX_VALUE,
                EmotionRangeTable.CREATED_AT,
            )
    }
}
