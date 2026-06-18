package com.firstpenguin.app.domain.recommendation.policy

import com.firstpenguin.app.domain.recommendation.model.IntentType
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.global.enums.EmotionRangeName
import com.firstpenguin.app.global.enums.TagType

@Suppress("MagicNumber")
object IntentFocusWeightPolicy {
    val weights: Map<IntentType, Map<TagType, Double>> =
        mapOf(
            IntentType.CONTEXT_BASED to
                mapOf(
                    TagType.CONTEXT to 0.40,
                    TagType.EMOTION to 0.20,
                    TagType.MOOD to 0.20,
                    TagType.SITUATION to 0.10,
                    TagType.NEED to 0.10,
                ),
            IntentType.EMOTION_NEED_BASED to
                mapOf(
                    TagType.EMOTION to 0.45,
                    TagType.NEED to 0.20,
                    TagType.MOOD to 0.15,
                    TagType.SITUATION to 0.10,
                    TagType.CONTEXT to 0.10,
                ),
            IntentType.SITUATION_BASED to
                mapOf(
                    TagType.SITUATION to 0.35,
                    TagType.EMOTION to 0.25,
                    TagType.NEED to 0.15,
                    TagType.MOOD to 0.15,
                    TagType.CONTEXT to 0.10,
                ),
            IntentType.MIXED to
                mapOf(
                    TagType.EMOTION to 0.40,
                    TagType.NEED to 0.20,
                    TagType.CONTEXT to 0.15,
                    TagType.MOOD to 0.15,
                    TagType.SITUATION to 0.10,
                ),
        )

    fun weightsOf(intentType: IntentType): Map<TagType, Double> = weights.getValue(intentType)

    fun weightsOf(
        input: RecommendationInput,
        intentType: IntentType,
    ): Map<TagType, Double> =
        weightsOf(intentType)
            .adjustedBy(input.emotionRangeName())

    fun weightOf(
        intentType: IntentType,
        tagType: TagType,
    ): Double = weightsOf(intentType)[tagType] ?: 0.0

    private fun Map<TagType, Double>.adjustedBy(emotionRangeName: EmotionRangeName): Map<TagType, Double> =
        when (emotionRangeName) {
            EmotionRangeName.SAD -> adjusted(SAD_MULTIPLIERS)
            EmotionRangeName.NORMAL -> this
            EmotionRangeName.HAPPY -> adjusted(HAPPY_MULTIPLIERS)
        }

    private fun Map<TagType, Double>.adjusted(multipliers: Map<TagType, Double>): Map<TagType, Double> {
        val adjustedWeights = mapValues { (type, weight) -> weight * multipliers.multiplierOf(type) }
        val totalWeight = adjustedWeights.values.sum()

        return adjustedWeights.mapValues { (_, weight) -> weight / totalWeight }
    }

    private fun Map<TagType, Double>.multiplierOf(tagType: TagType): Double = getOrDefault(tagType, DEFAULT_MULTIPLIER)

    private fun RecommendationInput.emotionRangeName(): EmotionRangeName =
        when (emotionValue) {
            in SAD_EMOTION_VALUE_RANGE -> EmotionRangeName.SAD
            in NORMAL_EMOTION_VALUE_RANGE -> EmotionRangeName.NORMAL
            in HAPPY_EMOTION_VALUE_RANGE -> EmotionRangeName.HAPPY
            else -> error("Unsupported emotion value: $emotionValue")
        }

    private val SAD_MULTIPLIERS =
        mapOf(
            TagType.NEED to 1.5,
            TagType.EMOTION to 0.85,
        )
    private val HAPPY_MULTIPLIERS =
        mapOf(
            TagType.EMOTION to 1.25,
            TagType.NEED to 0.75,
            TagType.MOOD to 0.75,
        )

    private const val DEFAULT_MULTIPLIER = 1.0
    private val SAD_EMOTION_VALUE_RANGE = 1..3
    private val NORMAL_EMOTION_VALUE_RANGE = 4..6
    private val HAPPY_EMOTION_VALUE_RANGE = 7..9
}
