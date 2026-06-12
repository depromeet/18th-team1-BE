package com.firstpenguin.app.domain.recommendation.model

data class AnalyzedUserIntent(
    val input: RecommendationInput,
    val type: IntentType,
    val queryText: String?,
    val tagCandidates: List<TagCandidate> = emptyList(),
)
