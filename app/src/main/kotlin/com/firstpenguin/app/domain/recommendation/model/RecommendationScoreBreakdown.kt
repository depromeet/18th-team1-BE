package com.firstpenguin.app.domain.recommendation.model

data class RecommendationScoreBreakdown(
    val needScore: Double,
    val emotionScore: Double,
    val contextScore: Double,
    val situationScore: Double,
    val moodScore: Double,
    val metadataScore: Double,
    val semanticScore: Double,
    val finalScore: Double,
)
