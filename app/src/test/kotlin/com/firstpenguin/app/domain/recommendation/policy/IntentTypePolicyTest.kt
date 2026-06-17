package com.firstpenguin.app.domain.recommendation.policy

import com.firstpenguin.app.domain.emotion.model.Tag
import com.firstpenguin.app.domain.recommendation.model.EffectiveTag
import com.firstpenguin.app.domain.recommendation.model.IntentType
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.global.enums.TagType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class IntentTypePolicyTest {
    @Test
    fun `context와 situation이 함께 있으면 mixed로 계산한다`() {
        val result =
            IntentTypePolicy.resolve(
                input = recommendationInput(),
                effectiveTags =
                    listOf(
                        effectiveTag(TagType.CONTEXT),
                        effectiveTag(TagType.SITUATION),
                    ),
            )

        assertEquals(IntentType.MIXED, result)
    }

    @Test
    fun `context tag가 있으면 context based로 계산한다`() {
        val result =
            IntentTypePolicy.resolve(
                input = recommendationInput(),
                effectiveTags = listOf(effectiveTag(TagType.CONTEXT)),
            )

        assertEquals(IntentType.CONTEXT_BASED, result)
    }

    @Test
    fun `situation tag가 있으면 situation based로 계산한다`() {
        val result =
            IntentTypePolicy.resolve(
                input = recommendationInput(),
                effectiveTags = listOf(effectiveTag(TagType.SITUATION)),
            )

        assertEquals(IntentType.SITUATION_BASED, result)
    }

    @Test
    fun `선택한 emotion need 중심이면 emotion need based로 계산한다`() {
        val result =
            IntentTypePolicy.resolve(
                input = recommendationInput(),
                effectiveTags = emptyList(),
            )

        assertEquals(IntentType.EMOTION_NEED_BASED, result)
    }

    private fun recommendationInput(): RecommendationInput =
        RecommendationInput(
            userId = USER_ID,
            emotionValue = EMOTION_VALUE,
            emotionRangeId = EMOTION_RANGE_ID,
            emotionTags = listOf(tag(TagType.EMOTION)),
            needTag = tag(TagType.NEED),
            feelingText = null,
            diaryText = null,
            analysis = null,
        )

    private fun tag(type: TagType): Tag =
        Tag(
            id = type.ordinal.toLong(),
            emotionRangeId = if (type == TagType.EMOTION) EMOTION_RANGE_ID else null,
            code = "${type.name}_CODE",
            label = type.name,
            type = type,
            createdAt = CREATED_AT,
        )

    private fun effectiveTag(type: TagType): EffectiveTag =
        EffectiveTag(
            tagId = type.ordinal.toLong(),
            code = "${type.name}_CODE",
            type = type,
        )

    private companion object {
        const val USER_ID = 1L
        const val EMOTION_RANGE_ID = 1L
        const val EMOTION_VALUE = 1
        val CREATED_AT: LocalDateTime = LocalDateTime.of(2026, 6, 16, 0, 0)
    }
}
