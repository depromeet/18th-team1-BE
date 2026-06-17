package com.firstpenguin.app.domain.recommendation.model

data class UserInputAnalysis(
    val canonicalIntent: String?,
    val tagCandidates: List<TagCandidate>,
    val llmModel: String? = null,
    val llmModelVersion: Int? = null,
    val inputTokens: Long? = null,
    val cachedTokens: Long? = null,
    val outputTokens: Long? = null,
    val llmElapsedMs: Long? = null,
)
