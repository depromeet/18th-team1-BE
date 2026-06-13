package com.firstpenguin.app.domain.recommendation.policy

import com.firstpenguin.app.domain.recommendation.model.IntentType
import com.firstpenguin.app.global.enums.TagType

@Suppress("MagicNumber")
object IntentFocusWeightPolicy {
    val weights: Map<IntentType, Map<TagType, Double>> =
        mapOf(
            IntentType.CONTEXT_BASED to
                mapOf(
                    TagType.CONTEXT to 0.40,
                    TagType.MOOD to 0.25,
                    TagType.NEED to 0.20,
                    TagType.EMOTION to 0.05,
                    TagType.SITUATION to 0.10,
                ),
            IntentType.EMOTION_NEED_BASED to
                mapOf(
                    TagType.NEED to 0.35,
                    TagType.EMOTION to 0.30,
                    TagType.MOOD to 0.15,
                    TagType.SITUATION to 0.10,
                    TagType.CONTEXT to 0.10,
                ),
            IntentType.SITUATION_BASED to
                mapOf(
                    TagType.SITUATION to 0.35,
                    TagType.NEED to 0.25,
                    TagType.EMOTION to 0.15,
                    TagType.MOOD to 0.15,
                    TagType.CONTEXT to 0.10,
                ),
            IntentType.MIXED to
                mapOf(
                    TagType.NEED to 0.30,
                    TagType.EMOTION to 0.25,
                    TagType.CONTEXT to 0.20,
                    TagType.MOOD to 0.15,
                    TagType.SITUATION to 0.10,
                ),
        )

    fun weightsOf(intentType: IntentType): Map<TagType, Double> = weights.getValue(intentType)

    fun weightOf(
        intentType: IntentType,
        tagType: TagType,
    ): Double = weightsOf(intentType)[tagType] ?: 0.0
}
