package com.firstpenguin.app.domain.recommendation.model

data class RecommendationAnalysisLog(
    val llmModel: String?,
    val llmModelVersion: Int?,
    val canonicalIntent: String?,
    val embeddingInputText: String?,
    val inputTokens: Long?,
    val cachedTokens: Long?,
    val outputTokens: Long?,
    val llmElapsedMs: Long?,
    val embeddingElapsedMs: Long?,
)
