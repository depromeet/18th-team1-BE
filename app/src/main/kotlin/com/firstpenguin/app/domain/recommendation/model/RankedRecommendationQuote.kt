package com.firstpenguin.app.domain.recommendation.model

data class RankedRecommendationQuote(
    val rank: Int,
    val candidate: RecommendationCandidate,
    val score: RecommendationScoreBreakdown,
    val source: RecommendationCandidateSource = RecommendationCandidateSource.PRIMARY,
) {
    val quoteId: Long
        get() = candidate.quoteId
}
