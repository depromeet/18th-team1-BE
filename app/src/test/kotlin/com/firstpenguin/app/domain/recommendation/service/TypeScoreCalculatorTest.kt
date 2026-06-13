package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.recommendation.model.EffectiveTag
import com.firstpenguin.app.global.enums.TagType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TypeScoreCalculatorTest {
    private val calculator = TypeScoreCalculator()

    @Test
    fun `bestMatch와 coverage를 합산해 type score를 계산한다`() {
        val targetTags =
            listOf(
                effectiveTag(tagId = 1L, importance = 0.8),
                effectiveTag(tagId = 2L, importance = 0.4),
                effectiveTag(tagId = 3L, importance = 0.2),
            )

        val result = calculator.calculate(targetTags, setOf(1L, 2L))

        assertEquals(0.814285, result, DELTA)
    }

    @Test
    fun `매칭되는 tag가 없으면 0점을 반환한다`() {
        val result = calculator.calculate(setOf(1L, 2L), setOf(3L))

        assertEquals(0.0, result, DELTA)
    }

    @Test
    fun `동일 가중치 tag id도 type score로 계산한다`() {
        val result = calculator.calculate(setOf(1L, 2L, 3L, 4L), setOf(1L, 2L))

        assertEquals(0.875, result, DELTA)
    }

    private fun effectiveTag(
        tagId: Long,
        importance: Double,
    ): EffectiveTag =
        EffectiveTag(
            tagId = tagId,
            code = "TAG_$tagId",
            type = TagType.NEED,
            importance = importance,
        )

    private companion object {
        const val DELTA = 0.000001
    }
}
