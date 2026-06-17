package com.firstpenguin.app.domain.recommendation.policy

import com.firstpenguin.app.domain.emotion.model.Tag
import com.firstpenguin.app.domain.recommendation.model.EffectiveTag
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.global.enums.TagType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class MoodTagPolicyTest {
    private val moodTagPolicy = MoodTagPolicy()

    @Test
    fun `needTag와 emotionTag와 effectiveTag 점수를 합산해 상위 mood code를 계산한다`() {
        val input =
            recommendationInput(
                emotionTags = listOf(tag(1L, TagType.EMOTION, "ANXIOUS")),
                needTag = tag(1L, TagType.NEED, "COMFORT"),
            )
        val effectiveTags =
            listOf(
                EffectiveTag(
                    tagId = 2L,
                    code = "FAILURE_MISTAKE",
                    type = TagType.SITUATION,
                    importance = 0.8,
                ),
                EffectiveTag(
                    tagId = 3L,
                    code = "RAIN",
                    type = TagType.CONTEXT,
                    importance = 0.4,
                ),
            )

        val result = moodTagPolicy.resolveMoodTagCodes(input, effectiveTags)

        assertEquals(
            listOf("MOOD_WARM", "MOOD_CALM_TONE", "MOOD_PLAIN", "MOOD_REALISTIC", "MOOD_GENTLE"),
            result.toList(),
        )
    }

    @Test
    fun `이미 prefix가 붙은 tag code도 점수 계산에 사용한다`() {
        val input =
            recommendationInput(
                emotionTags = listOf(tag(1L, TagType.EMOTION, "EMOTION_HAPPY")),
                needTag = tag(1L, TagType.NEED, "NEED_INSPIRATION"),
            )
        val effectiveTags =
            listOf(
                EffectiveTag(
                    tagId = 2L,
                    code = "CONTEXT_NIGHT",
                    type = TagType.CONTEXT,
                    importance = 0.7,
                ),
            )

        val result = moodTagPolicy.resolveMoodTagCodes(input, effectiveTags)

        assertEquals(
            listOf("MOOD_POETIC", "MOOD_HOPEFUL", "MOOD_LIGHT", "MOOD_REALISTIC", "MOOD_CALM_TONE"),
            result.toList(),
        )
    }

    @Test
    fun `사용자가 고른 tag가 effectiveTag에 있어도 중복 계산하지 않는다`() {
        val input =
            recommendationInput(
                emotionTags = listOf(tag(1L, TagType.EMOTION, "ANXIOUS")),
                needTag = tag(2L, TagType.NEED, "COMFORT"),
            )
        val effectiveTags =
            listOf(
                EffectiveTag(
                    tagId = 1L,
                    code = "EMOTION_ANXIOUS",
                    type = TagType.EMOTION,
                    importance = 1.0,
                ),
                EffectiveTag(
                    tagId = 2L,
                    code = "NEED_COMFORT",
                    type = TagType.NEED,
                    importance = 1.0,
                ),
            )

        val result = moodTagPolicy.resolveMoodTagCodes(input, effectiveTags)

        assertEquals(
            listOf("MOOD_CALM_TONE", "MOOD_WARM", "MOOD_PLAIN", "MOOD_GENTLE", "MOOD_REALISTIC"),
            result.toList(),
        )
    }

    private fun recommendationInput(
        emotionTags: List<Tag>,
        needTag: Tag?,
    ): RecommendationInput =
        RecommendationInput(
            userId = USER_ID,
            emotionValue = SAD_EMOTION_VALUE,
            emotionRangeId = SAD_EMOTION_RANGE_ID,
            emotionTags = emotionTags,
            needTag = needTag,
            feelingText = null,
            diaryText = null,
            analysis = null,
        )

    private fun tag(
        id: Long,
        type: TagType,
        code: String,
    ): Tag =
        Tag(
            id = id,
            emotionRangeId = if (type == TagType.EMOTION) SAD_EMOTION_RANGE_ID else null,
            code = code,
            label = code,
            type = type,
            createdAt = CREATED_AT,
        )

    private companion object {
        const val USER_ID = 1L
        const val SAD_EMOTION_RANGE_ID = 1L
        const val SAD_EMOTION_VALUE = 1
        val CREATED_AT: LocalDateTime = LocalDateTime.of(2026, 6, 13, 0, 0)
    }
}
