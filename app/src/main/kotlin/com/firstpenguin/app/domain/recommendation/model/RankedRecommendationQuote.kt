package com.firstpenguin.app.domain.recommendation.model

data class RankedRecommendationQuote(
    val rank: Int,
    val candidate: RecommendationCandidate,
    val score: Double,
) {
    val quoteId: Long
        get() = candidate.quoteId
}
