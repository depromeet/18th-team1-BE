package com.firstpenguin.app.domain.recommendation.model

data class SourcedRecommendationCandidate(
    val candidate: RecommendationCandidate,
    val source: RecommendationCandidateSource,
) {
    val quoteId: Long
        get() = candidate.quoteId
}
