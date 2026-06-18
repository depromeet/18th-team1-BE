package com.firstpenguin.app.domain.recommendation.policy

import com.firstpenguin.app.domain.emotion.model.Tag
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.domain.recommendation.model.TagCandidatePriority
import com.firstpenguin.app.domain.recommendation.model.TagCandidateSource
import com.firstpenguin.app.global.enums.TagType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class TagCandidatePriorityPolicyTest {
    @Test
    fun `선택 need가 없고 의도 텍스트에서 나온 need는 primary로 계산한다`() {
        val result =
            TagCandidatePriorityPolicy.resolve(
                input = recommendationInput(needTag = null),
                tagType = TagType.NEED,
                source = TagCandidateSource.FEELING_TEXT,
            )

        assertEquals(TagCandidatePriority.PRIMARY, result)
    }

    @Test
    fun `선택 need가 있으면 추가 need 후보는 secondary로 계산한다`() {
        val result =
            TagCandidatePriorityPolicy.resolve(
                input = recommendationInput(needTag = tag(TagType.NEED)),
                tagType = TagType.NEED,
                source = TagCandidateSource.FEELING_TEXT,
            )

        assertEquals(TagCandidatePriority.SECONDARY, result)
    }

    @Test
    fun `의도 텍스트가 있으면 diary 기반 need 후보는 secondary로 계산한다`() {
        val result =
            TagCandidatePriorityPolicy.resolve(
                input = recommendationInput(feelingText = "위로받고 싶어", needTag = null),
                tagType = TagType.NEED,
                source = TagCandidateSource.DIARY_TEXT,
            )

        assertEquals(TagCandidatePriority.SECONDARY, result)
    }

    @Test
    fun `diary만 있으면 diary 기반 need 후보는 primary로 계산한다`() {
        val result =
            TagCandidatePriorityPolicy.resolve(
                input = recommendationInput(feelingText = null, diaryText = "위로받고 싶다", needTag = null),
                tagType = TagType.NEED,
                source = TagCandidateSource.DIARY_TEXT,
            )

        assertEquals(TagCandidatePriority.PRIMARY, result)
    }

    @Test
    fun `의도 텍스트가 있으면 diary 기반 context situation 후보는 background로 계산한다`() {
        listOf(TagType.CONTEXT, TagType.SITUATION).forEach { tagType ->
            val result =
                TagCandidatePriorityPolicy.resolve(
                    input = recommendationInput(feelingText = "행복해서 좋은 문장이 필요해"),
                    tagType = tagType,
                    source = TagCandidateSource.DIARY_TEXT,
                )

            assertEquals(TagCandidatePriority.BACKGROUND, result)
        }
    }

    @Test
    fun `의도 텍스트나 diary 단독 기반 context situation 후보는 secondary로 계산한다`() {
        listOf(
            recommendationInput(feelingText = "회사 일 때문에 지쳤어") to TagCandidateSource.FEELING_TEXT,
            recommendationInput(feelingText = null, diaryText = "회사 일이 힘들었다") to TagCandidateSource.DIARY_TEXT,
        ).forEach { (input, source) ->
            val result = TagCandidatePriorityPolicy.resolve(input, TagType.SITUATION, source)

            assertEquals(TagCandidatePriority.SECONDARY, result)
        }
    }

    private fun recommendationInput(
        feelingText: String? = "위로받고 싶어",
        diaryText: String? = "오늘은 비가 왔다",
        needTag: Tag? = null,
    ): RecommendationInput =
        RecommendationInput(
            userId = USER_ID,
            emotionValue = EMOTION_VALUE,
            emotionRangeId = EMOTION_RANGE_ID,
            emotionTags = emptyList(),
            needTag = needTag,
            feelingText = feelingText,
            diaryText = diaryText,
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

    private companion object {
        const val USER_ID = 1L
        const val EMOTION_RANGE_ID = 1L
        const val EMOTION_VALUE = 1
        val CREATED_AT: LocalDateTime = LocalDateTime.of(2026, 6, 18, 0, 0)
    }
}
