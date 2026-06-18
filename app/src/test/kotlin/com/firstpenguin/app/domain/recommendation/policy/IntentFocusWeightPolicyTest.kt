package com.firstpenguin.app.domain.recommendation.policy

import com.firstpenguin.app.domain.recommendation.model.IntentType
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.global.enums.TagType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IntentFocusWeightPolicyTest {
    @Test
    fun `모든 intent type에서 emotion weight는 need weight보다 크다`() {
        IntentType.entries.forEach { intentType ->
            val weights = IntentFocusWeightPolicy.weightsOf(intentType)

            assertTrue(weights.getValue(TagType.EMOTION) > weights.getValue(TagType.NEED))
        }
    }

    @Test
    fun `모든 intent type의 focus weight 합은 1이다`() {
        IntentType.entries.forEach { intentType ->
            val weights = IntentFocusWeightPolicy.weightsOf(intentType)

            assertEquals(FULL_WEIGHT, weights.values.sum(), DELTA)
        }
    }

    @Test
    fun `sad 입력은 need weight를 높인다`() {
        val normalWeights =
            IntentFocusWeightPolicy.weightsOf(input(NORMAL_EMOTION_VALUE), IntentType.EMOTION_NEED_BASED)
        val sadWeights =
            IntentFocusWeightPolicy.weightsOf(input(SAD_EMOTION_VALUE), IntentType.EMOTION_NEED_BASED)

        assertTrue(sadWeights.getValue(TagType.NEED) > normalWeights.getValue(TagType.NEED))
    }

    @Test
    fun `happy 입력은 emotion weight를 높이고 mood와 need weight를 낮춘다`() {
        val normalWeights =
            IntentFocusWeightPolicy.weightsOf(input(NORMAL_EMOTION_VALUE), IntentType.EMOTION_NEED_BASED)
        val happyWeights =
            IntentFocusWeightPolicy.weightsOf(input(HAPPY_EMOTION_VALUE), IntentType.EMOTION_NEED_BASED)

        assertTrue(happyWeights.getValue(TagType.EMOTION) > normalWeights.getValue(TagType.EMOTION))
        assertTrue(happyWeights.getValue(TagType.NEED) < normalWeights.getValue(TagType.NEED))
        assertTrue(happyWeights.getValue(TagType.MOOD) < normalWeights.getValue(TagType.MOOD))
    }

    @Test
    fun `emotion value별로 조정된 focus weight 합은 1이다`() {
        listOf(SAD_EMOTION_VALUE, NORMAL_EMOTION_VALUE, HAPPY_EMOTION_VALUE).forEach { emotionValue ->
            IntentType.entries.forEach { intentType ->
                val weights = IntentFocusWeightPolicy.weightsOf(input(emotionValue), intentType)

                assertEquals(FULL_WEIGHT, weights.values.sum(), DELTA)
            }
        }
    }

    @Test
    fun `normal 입력은 기본 focus weight를 유지한다`() {
        IntentType.entries.forEach { intentType ->
            assertEquals(
                IntentFocusWeightPolicy.weightsOf(intentType),
                IntentFocusWeightPolicy.weightsOf(input(NORMAL_EMOTION_VALUE), intentType),
            )
        }
    }

    private fun input(emotionValue: Int): RecommendationInput =
        RecommendationInput(
            userId = USER_ID,
            emotionValue = emotionValue,
            emotionRangeId = EMOTION_RANGE_ID,
            emotionTags = emptyList(),
            needTag = null,
            feelingText = null,
            diaryText = null,
            analysis = null,
        )

    private companion object {
        const val USER_ID = 1L
        const val EMOTION_RANGE_ID = 1L
        const val SAD_EMOTION_VALUE = 1
        const val NORMAL_EMOTION_VALUE = 5
        const val HAPPY_EMOTION_VALUE = 8
        const val FULL_WEIGHT = 1.0
        const val DELTA = 0.000001
    }
}
