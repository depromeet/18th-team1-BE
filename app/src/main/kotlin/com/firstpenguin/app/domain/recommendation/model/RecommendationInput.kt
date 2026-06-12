package com.firstpenguin.app.domain.recommendation.model

data class RecommendationInput(
    val userId: Long,
    val emotionRangeId: Long,
    val emotionTagIds: List<Long>,
    val needTagId: Long?,
    val feelingText: String?,
    val diaryText: String?,
)
