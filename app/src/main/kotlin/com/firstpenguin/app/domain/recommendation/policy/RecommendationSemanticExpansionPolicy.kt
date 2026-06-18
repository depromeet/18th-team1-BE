package com.firstpenguin.app.domain.recommendation.policy

import com.firstpenguin.app.domain.recommendation.model.EffectiveTag
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.domain.recommendation.model.UserSemanticEmbedding
import com.firstpenguin.app.global.enums.TagType

object RecommendationSemanticExpansionPolicy {
    fun decide(
        input: RecommendationInput,
        effectiveTags: Collection<EffectiveTag>,
        userEmbedding: UserSemanticEmbedding?,
    ): RecommendationSemanticExpansionDecision =
        when {
            userEmbedding == null || !input.hasAnalysisText() -> RecommendationSemanticExpansionDecision.NONE
            effectiveTags.hasSpecificSemanticTag() -> RecommendationSemanticExpansionDecision.SPECIFIC
            input.isShortAnalysisText() -> RecommendationSemanticExpansionDecision.NONE
            input.diaryText.hasValue() -> RecommendationSemanticExpansionDecision.SPECIFIC
            else -> RecommendationSemanticExpansionDecision.NONE
        }

    private fun Collection<EffectiveTag>.hasSpecificSemanticTag(): Boolean = any(::isSpecificSemanticTag)

    private fun isSpecificSemanticTag(tag: EffectiveTag): Boolean = tag.type in SPECIFIC_SEMANTIC_TAG_TYPES

    private fun RecommendationInput.hasAnalysisText(): Boolean = feelingText.hasValue() || diaryText.hasValue()

    private fun RecommendationInput.textLength(): Int = analysisTexts().sumOf(String::length)

    private fun RecommendationInput.isShortAnalysisText(): Boolean = textLength() < SEMANTIC_EXPANSION_MIN_TEXT_LENGTH

    private fun RecommendationInput.analysisTexts(): List<String> = listOfNotNull(feelingText, diaryText)

    private fun String?.hasValue(): Boolean = !isNullOrBlank()

    private val SPECIFIC_SEMANTIC_TAG_TYPES = setOf(TagType.CONTEXT, TagType.SITUATION)
    private const val SEMANTIC_EXPANSION_MIN_TEXT_LENGTH = 20
}

data class RecommendationSemanticExpansionDecision(
    val seedLimit: Int?,
    val prioritizeFallback: Boolean,
    val requireMetadataForStrongSemantic: Boolean,
) {
    companion object {
        val NONE =
            RecommendationSemanticExpansionDecision(
                seedLimit = null,
                prioritizeFallback = false,
                requireMetadataForStrongSemantic = true,
            )
        val SPECIFIC =
            RecommendationSemanticExpansionDecision(
                seedLimit = SPECIFIC_SEMANTIC_SEED_LIMIT,
                prioritizeFallback = true,
                requireMetadataForStrongSemantic = false,
            )
        private const val SPECIFIC_SEMANTIC_SEED_LIMIT = 30
    }
}
