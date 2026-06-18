package com.firstpenguin.app.domain.recommendation.policy

import com.firstpenguin.app.domain.recommendation.model.EffectiveTag
import com.firstpenguin.app.domain.recommendation.model.RecommendationCandidate
import com.firstpenguin.app.domain.recommendation.model.RecommendationFinalScoreWeights
import com.firstpenguin.app.global.enums.TagType

private typealias TagKey = Pair<TagType, Long>

object RecommendationFinalScoreWeightPolicy {
    fun weightsOf(
        effectiveTags: Collection<EffectiveTag>,
        candidates: Collection<RecommendationCandidate>,
    ): RecommendationFinalScoreWeights {
        val specificTagKeys = effectiveTags.specificTagKeys()
        val coverageRatio = candidates.coverageRatioOrNull(specificTagKeys)

        return when {
            coverageRatio == null -> RecommendationFinalScoreWeights.DEFAULT
            coverageRatio <= NO_COVERAGE_RATIO -> RecommendationFinalScoreWeights.SEMANTIC_FOCUSED
            coverageRatio < LOW_COVERAGE_RATIO -> RecommendationFinalScoreWeights.SEMANTIC_LEANING
            else -> RecommendationFinalScoreWeights.DEFAULT
        }
    }

    private fun Collection<EffectiveTag>.specificTagKeys(): Set<TagKey> =
        filter { tag -> tag.type in SPECIFIC_TAG_TYPES }
            .map { tag -> tag.type to tag.tagId }
            .toSet()

    private fun Collection<RecommendationCandidate>.coverageRatioOrNull(specificTagKeys: Set<TagKey>): Double? {
        if (specificTagKeys.isEmpty() || isEmpty()) return null

        return count { candidate -> candidate.matchesAny(specificTagKeys) }.toDouble() / size
    }

    private fun RecommendationCandidate.matchesAny(specificTagKeys: Set<TagKey>): Boolean =
        specificTagKeys.any { (type, tagId) -> tagId in tagIdsByType[type].orEmpty() }

    private val SPECIFIC_TAG_TYPES = setOf(TagType.CONTEXT, TagType.SITUATION)
    private const val NO_COVERAGE_RATIO = 0.0
    private const val LOW_COVERAGE_RATIO = 0.15
}
