package com.firstpenguin.app.domain.recommendation.model

import java.time.LocalDateTime

data class DailyRecommendationQuote(
    val id: Long,
    val dailyRecommendationId: Long,
    val quoteId: Long,
    val displayOrder: Int,
    val createdAt: LocalDateTime,
)
