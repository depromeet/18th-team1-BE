package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.emotion.model.Tag
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.domain.recommendation.model.UserInputAnalysis
import com.firstpenguin.app.global.enums.TagType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class UserEmbeddingInputBuilderTest {
    private val builder = UserEmbeddingInputBuilder()

    @Test
    fun `canonicalIntent가 있으면 trim해서 embedding input으로 사용한다`() {
        val result =
            builder.build(
                recommendationInput(canonicalIntent = "  불안한 마음을 진정시키고 싶다  "),
            )

        assertEquals("불안한 마음을 진정시키고 싶다", result)
    }

    @Test
    fun `canonicalIntent가 없으면 사용자 입력 텍스트를 embedding input으로 사용한다`() {
        val result =
            builder.build(
                recommendationInput(
                    canonicalIntent = null,
                    feelingText = "  비 오는 출근길이 불안해  ",
                    diaryText = "회사에 늦을까 걱정했다",
                ),
            )

        assertEquals("feelingText: 비 오는 출근길이 불안해\ndiaryText: 회사에 늦을까 걱정했다", result)
    }

    @Test
    fun `canonicalIntent가 blank면 사용자 입력 텍스트를 embedding input으로 사용한다`() {
        val result =
            builder.build(
                recommendationInput(
                    canonicalIntent = " ",
                    feelingText = "불안해",
                ),
            )

        assertEquals("feelingText: 불안해", result)
    }

    @Test
    fun `canonicalIntent와 사용자 입력 텍스트가 모두 없으면 embedding input을 만들지 않는다`() {
        val result = builder.build(recommendationInput(canonicalIntent = null))

        assertNull(result)
    }

    private companion object {
        const val USER_ID = 1L
        const val EMOTION_RANGE_ID = 1L
        const val EMOTION_VALUE = 1
        const val EMOTION_TAG_ID = 10L
        const val NEED_TAG_ID = 20L
        val CREATED_AT: LocalDateTime = LocalDateTime.of(2026, 6, 14, 0, 0)

        fun recommendationInput(
            canonicalIntent: String?,
            feelingText: String? = null,
            diaryText: String? = null,
        ): RecommendationInput =
            RecommendationInput(
                userId = USER_ID,
                emotionValue = EMOTION_VALUE,
                emotionRangeId = EMOTION_RANGE_ID,
                emotionTags = listOf(tag(EMOTION_TAG_ID, TagType.EMOTION, "불안")),
                needTag = tag(NEED_TAG_ID, TagType.NEED, "위로"),
                feelingText = feelingText,
                diaryText = diaryText,
                analysis =
                    UserInputAnalysis(
                        canonicalIntent = canonicalIntent,
                        tagCandidates = emptyList(),
                    ),
            )

        fun tag(
            id: Long,
            type: TagType,
            label: String,
        ): Tag =
            Tag(
                id = id,
                emotionRangeId = if (type == TagType.EMOTION) EMOTION_RANGE_ID else null,
                code = "${type.name}_$id",
                label = label,
                type = type,
                createdAt = CREATED_AT,
            )
    }
}
