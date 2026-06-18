package com.firstpenguin.app.domain.recommendation.dto

import jakarta.validation.Validation
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RecommendationRequestTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `직접 입력 문장은 최대 30자까지 허용한다`() {
        val request = recommendationRequest(feelingText = "가".repeat(MAX_FEELING_TEXT_LENGTH + 1))

        val messages = validator.validate(request).map { violation -> violation.message }

        assertTrue("직접 입력 문장은 최대 30자까지 입력할 수 있습니다." in messages)
    }

    @Test
    fun `일기 내용은 최대 300자까지 허용한다`() {
        val request = recommendationRequest(diaryText = "가".repeat(MAX_DIARY_TEXT_LENGTH + 1))

        val messages = validator.validate(request).map { violation -> violation.message }

        assertTrue("일기 내용은 최대 300자까지 입력할 수 있습니다." in messages)
    }

    private fun recommendationRequest(
        feelingText: String? = null,
        diaryText: String? = null,
    ): RecommendationRequest =
        RecommendationRequest(
            emotionValue = EMOTION_VALUE,
            emotionTagIds = listOf(EMOTION_TAG_ID),
            needTagId = NEED_TAG_ID,
            feelingText = feelingText,
            diaryText = diaryText,
        )

    private companion object {
        const val EMOTION_VALUE = 5
        const val EMOTION_TAG_ID = 1L
        const val NEED_TAG_ID = 2L
        const val MAX_FEELING_TEXT_LENGTH = 30
        const val MAX_DIARY_TEXT_LENGTH = 300
    }
}
