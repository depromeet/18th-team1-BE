package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.recommendation.model.EffectiveTag
import org.springframework.stereotype.Component

@Suppress("MagicNumber")
@Component
class TypeScoreCalculator {
    fun calculate(
        targetTags: Collection<EffectiveTag>,
        candidateTagIds: Set<Long>,
        tagRarityWeights: Map<Long, Double> = emptyMap(),
    ): Double {
        if (targetTags.isEmpty() || candidateTagIds.isEmpty()) return NO_MATCH_SCORE

        val totalImportance = targetTags.sumOf { tag -> tag.importance }
        return if (totalImportance <= NO_MATCH_SCORE) {
            NO_MATCH_SCORE
        } else {
            calculateByImportance(targetTags, candidateTagIds, totalImportance, tagRarityWeights)
        }
    }

    fun calculate(
        targetTagIds: Set<Long>,
        candidateTagIds: Set<Long>,
        tagRarityWeights: Map<Long, Double> = emptyMap(),
    ): Double {
        if (targetTagIds.isEmpty() || candidateTagIds.isEmpty()) return NO_MATCH_SCORE

        val matchedWeights =
            targetTagIds
                .filter { tagId -> tagId in candidateTagIds }
                .map { tagId -> tagRarityWeights.rarityWeight(tagId) }
        val bestMatchScore = matchedWeights.maxOrNull() ?: NO_MATCH_SCORE
        val coverageScore = matchedWeights.sum() / targetTagIds.size

        return calculate(bestMatchScore, coverageScore)
    }

    private fun calculate(
        bestMatchScore: Double,
        coverageScore: Double,
    ): Double =
        BEST_MATCH_WEIGHT * bestMatchScore.coerceIn(NO_MATCH_SCORE, FULL_MATCH_SCORE) +
            COVERAGE_WEIGHT * coverageScore.coerceIn(NO_MATCH_SCORE, FULL_MATCH_SCORE)

    private fun calculateByImportance(
        targetTags: Collection<EffectiveTag>,
        candidateTagIds: Set<Long>,
        totalImportance: Double,
        tagRarityWeights: Map<Long, Double>,
    ): Double {
        val matchedWeights =
            targetTags
                .filter { tag -> tag.tagId in candidateTagIds }
                .map { tag -> tag.importance * tagRarityWeights.rarityWeight(tag.tagId) }
        val bestMatchScore = matchedWeights.maxOrNull() ?: NO_MATCH_SCORE
        val coverageScore = matchedWeights.sum() / totalImportance

        return calculate(bestMatchScore, coverageScore)
    }

    private fun Map<Long, Double>.rarityWeight(tagId: Long): Double =
        getOrDefault(tagId, FULL_MATCH_SCORE).coerceIn(NO_MATCH_SCORE, FULL_MATCH_SCORE)

    private companion object {
        const val BEST_MATCH_WEIGHT = 0.75
        const val COVERAGE_WEIGHT = 0.25
        const val NO_MATCH_SCORE = 0.0
        const val FULL_MATCH_SCORE = 1.0
    }
}
