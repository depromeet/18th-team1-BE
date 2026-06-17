package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.emotion.model.Tag
import com.firstpenguin.app.domain.recommendation.model.IntentType
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.domain.recommendation.model.TagCandidate
import com.firstpenguin.app.domain.recommendation.model.TagCandidatePriority
import com.firstpenguin.app.domain.recommendation.model.TagCandidateSource
import com.firstpenguin.app.domain.recommendation.model.UserInputAnalysis
import com.firstpenguin.app.global.enums.TagType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class EffectiveTagBuilderTest {
    private val effectiveTagBuilder = EffectiveTagBuilder()

    @Test
    fun `선택 태그는 1점으로 반영하고 낮은 LLM 후보는 제거한다`() {
        val input =
            recommendationInput(
                emotionTags = listOf(tag(1L, TagType.EMOTION, "EMOTION_SELF_BLAME")),
                needTag = tag(2L, TagType.NEED, "NEED_COMFORT"),
                tagCandidates =
                    listOf(
                        tagCandidate(3L, TagType.SITUATION, "SITUATION_FAILURE_MISTAKE", confidence = 0.95),
                        tagCandidate(
                            tagId = 4L,
                            type = TagType.CONTEXT,
                            code = "CONTEXT_RAIN",
                            source = TagCandidateSource.DIARY_TEXT,
                            priority = TagCandidatePriority.BACKGROUND,
                            confidence = 0.5,
                        ),
                    ),
            )

        val result = effectiveTagBuilder.build(input).associateBy { tag -> tag.tagId }

        assertEquals(3, result.size)
        assertEquals(1.0, result.getValue(1L).importance, DELTA)
        assertEquals(1.0, result.getValue(2L).importance, DELTA)
        assertEquals(0.8075, result.getValue(3L).importance, DELTA)
        assertFalse(result.containsKey(4L))
    }

    @Test
    fun `같은 tagId와 type은 하나로 병합한다`() {
        val input =
            recommendationInput(
                emotionTags = listOf(tag(1L, TagType.EMOTION, "EMOTION_SELF_BLAME")),
                tagCandidates =
                    listOf(
                        tagCandidate(1L, TagType.EMOTION, "EMOTION_SELF_BLAME", confidence = 0.8),
                    ),
            )

        val result = effectiveTagBuilder.build(input)

        assertEquals(1, result.size)
        assertEquals(1L, result.first().tagId)
        assertEquals(1.0, result.first().importance, DELTA)
    }

    private fun recommendationInput(
        emotionTags: List<Tag> = emptyList(),
        needTag: Tag? = null,
        tagCandidates: List<TagCandidate> = emptyList(),
    ): RecommendationInput =
        RecommendationInput(
            userId = USER_ID,
            emotionRangeId = SAD_EMOTION_RANGE_ID,
            emotionTags = emotionTags,
            needTag = needTag,
            feelingText = null,
            diaryText = null,
            analysis =
                UserInputAnalysis(
                    intentType = IntentType.EMOTION_NEED_BASED,
                    canonicalIntent = null,
                    tagCandidates = tagCandidates,
                ),
        )

    private fun tagCandidate(
        tagId: Long,
        type: TagType,
        code: String,
        source: TagCandidateSource = TagCandidateSource.FEELING_TEXT,
        priority: TagCandidatePriority = TagCandidatePriority.PRIMARY,
        confidence: Double,
    ): TagCandidate =
        TagCandidate(
            tagId = tagId,
            code = code,
            label = code,
            type = type,
            source = source,
            priority = priority,
            confidence = confidence,
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
        const val DELTA = 0.000001
        val CREATED_AT: LocalDateTime = LocalDateTime.of(2026, 6, 13, 0, 0)
    }
}
