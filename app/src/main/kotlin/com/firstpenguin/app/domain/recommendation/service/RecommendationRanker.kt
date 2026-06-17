package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.recommendation.model.RankedRecommendationQuote
import com.firstpenguin.app.domain.recommendation.model.RecommendationCandidate
import com.firstpenguin.app.domain.recommendation.model.RecommendationScoreBreakdown
import org.springframework.stereotype.Component

@Component
class RecommendationRanker {
    fun rank(
        candidates: List<RecommendationCandidate>,
        useSemanticScore: Boolean = true,
        scoreOf: (RecommendationCandidate) -> RecommendationScoreBreakdown,
    ): List<RankedRecommendationQuote> =
        candidates
            .map { candidate -> candidate to scoreOf(candidate).withFinalScore(useSemanticScore) }
            .sortedWith(
                compareByDescending<Pair<RecommendationCandidate, RecommendationScoreBreakdown>> {
                    it.second.finalScore
                }.thenByDescending { it.second.metadataScore }
                    .thenBy { it.first.quoteId },
            ).mapIndexed { index, (candidate, score) ->
                RankedRecommendationQuote(
                    rank = index + FIRST_RANK,
                    candidate = candidate,
                    score = score,
                )
            }

    private fun RecommendationScoreBreakdown.withFinalScore(useSemanticScore: Boolean): RecommendationScoreBreakdown {
        if (!useSemanticScore) return copy(finalScore = metadataScore)

        return copy(finalScore = METADATA_WEIGHT * metadataScore + SEMANTIC_WEIGHT * semanticScore)
    }

    private companion object {
        const val FIRST_RANK = 1
        const val METADATA_WEIGHT = 0.55
        const val SEMANTIC_WEIGHT = 0.45
    }
}
