package com.firstpenguin.app.domain.recommendation.model

data class RecommendationCandidate(
    val quoteId: Long,
    val score: Double,
    val effectiveTags: List<EffectiveTag> = emptyList(),
    val reason: String? = null,
)
