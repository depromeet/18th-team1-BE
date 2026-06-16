package com.firstpenguin.app.domain.recommendation.model

data class RecommendationResult(
    val mainQuote: RankedRecommendationQuote,
    val quotes: List<RankedRecommendationQuote>,
    val analysisLog: RecommendationAnalysisLog? = null,
)
