package com.firstpenguin.app.domain.recommendation.model

import java.time.LocalDateTime

data class DailyRecommendationTag(
    val id: Long,
    val dailyRecommendationId: Long,
    val tagId: Long,
    val createdAt: LocalDateTime,
)
