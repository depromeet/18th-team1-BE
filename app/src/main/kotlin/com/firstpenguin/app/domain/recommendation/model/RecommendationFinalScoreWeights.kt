package com.firstpenguin.app.domain.recommendation.model

data class RecommendationFinalScoreWeights(
    val metadata: Double,
    val semantic: Double,
) {
    companion object {
        val DEFAULT = RecommendationFinalScoreWeights(metadata = 0.45, semantic = 0.55)
        val SEMANTIC_LEANING = RecommendationFinalScoreWeights(metadata = 0.35, semantic = 0.65)
        val SEMANTIC_FOCUSED = RecommendationFinalScoreWeights(metadata = 0.30, semantic = 0.70)
    }
}
