package com.firstpenguin.app.domain.recommendation.model

data class UserInputAnalysis(
    val intentType: IntentType,
    val canonicalIntent: String?,
    val tagCandidates: List<TagCandidate>,
)
