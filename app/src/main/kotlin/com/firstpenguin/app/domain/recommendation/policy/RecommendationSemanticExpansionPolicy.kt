package com.firstpenguin.app.domain.recommendation.policy

import com.firstpenguin.app.domain.recommendation.model.EffectiveTag
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.domain.recommendation.model.UserSemanticEmbedding
import com.firstpenguin.app.global.enums.TagType

object RecommendationSemanticExpansionPolicy {
    fun shouldExpand(
        input: RecommendationInput,
        effectiveTags: Collection<EffectiveTag>,
        userEmbedding: UserSemanticEmbedding?,
    ): Boolean =
        when {
            userEmbedding == null || !input.hasAnalysisText() -> false
            effectiveTags.hasSpecificSemanticTag() -> true
            input.diaryText.hasValue() -> true
            input.isSadEmotionRange() -> true
            input.isHappyEmotionRange() -> false
            else -> input.analysisTextLength() >= SEMANTIC_EXPANSION_MIN_TEXT_LENGTH
        }

    private fun Collection<EffectiveTag>.hasSpecificSemanticTag(): Boolean = any(::isSpecificSemanticTag)

    private fun isSpecificSemanticTag(tag: EffectiveTag): Boolean = tag.type in SPECIFIC_SEMANTIC_TAG_TYPES

    private fun RecommendationInput.hasAnalysisText(): Boolean = feelingText.hasValue() || diaryText.hasValue()

    private fun RecommendationInput.analysisTextLength(): Int = analysisTexts().sumOf(String::length)

    private fun RecommendationInput.analysisTexts(): List<String> = listOfNotNull(feelingText, diaryText)

    private fun RecommendationInput.isSadEmotionRange(): Boolean = emotionValue in SAD_EMOTION_VALUE_RANGE

    private fun RecommendationInput.isHappyEmotionRange(): Boolean = emotionValue in HAPPY_EMOTION_VALUE_RANGE

    private fun String?.hasValue(): Boolean = !isNullOrBlank()

    private val SPECIFIC_SEMANTIC_TAG_TYPES = setOf(TagType.CONTEXT, TagType.SITUATION)
    private val SAD_EMOTION_VALUE_RANGE = SAD_EMOTION_VALUE_MIN..SAD_EMOTION_VALUE_MAX
    private val HAPPY_EMOTION_VALUE_RANGE = HAPPY_EMOTION_VALUE_MIN..HAPPY_EMOTION_VALUE_MAX
    private const val SAD_EMOTION_VALUE_MIN = 1
    private const val SAD_EMOTION_VALUE_MAX = 3
    private const val HAPPY_EMOTION_VALUE_MIN = 7
    private const val HAPPY_EMOTION_VALUE_MAX = 9
    private const val SEMANTIC_EXPANSION_MIN_TEXT_LENGTH = 20
}
