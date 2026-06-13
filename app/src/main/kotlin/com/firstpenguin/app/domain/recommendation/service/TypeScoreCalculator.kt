package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.recommendation.model.EffectiveTag
import org.springframework.stereotype.Component

@Suppress("MagicNumber")
@Component
class TypeScoreCalculator {
    fun calculate(
        targetTags: Collection<EffectiveTag>,
        candidateTagIds: Set<Long>,
    ): Double {
        if (targetTags.isEmpty() || candidateTagIds.isEmpty()) return NO_MATCH_SCORE

        val totalImportance = targetTags.sumOf { tag -> tag.importance }
        return if (totalImportance <= NO_MATCH_SCORE) {
            NO_MATCH_SCORE
        } else {
            calculateByImportance(targetTags, candidateTagIds, totalImportance)
        }
    }

    fun calculate(
        targetTagIds: Set<Long>,
        candidateTagIds: Set<Long>,
    ): Double {
        if (targetTagIds.isEmpty() || candidateTagIds.isEmpty()) return NO_MATCH_SCORE

        val matchCount = targetTagIds.count { tagId -> tagId in candidateTagIds }
        val bestMatchScore = if (matchCount > 0) FULL_MATCH_SCORE else NO_MATCH_SCORE
        val coverageScore = matchCount.toDouble() / targetTagIds.size

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
    ): Double {
        val matchedTags = targetTags.filter { tag -> tag.tagId in candidateTagIds }
        val bestMatchScore = matchedTags.maxOfOrNull { tag -> tag.importance } ?: NO_MATCH_SCORE
        val coverageScore = matchedTags.sumOf { tag -> tag.importance } / totalImportance

        return calculate(bestMatchScore, coverageScore)
    }

    private companion object {
        const val BEST_MATCH_WEIGHT = 0.75
        const val COVERAGE_WEIGHT = 0.25
        const val NO_MATCH_SCORE = 0.0
        const val FULL_MATCH_SCORE = 1.0
    }
}
