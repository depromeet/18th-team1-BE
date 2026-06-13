package com.firstpenguin.app.domain.recommendation.model

import java.time.LocalDateTime

data class RecommendationQuote(
    val id: Long,
    val recommendationId: Long,
    val quoteId: Long,
    val displayOrder: Int,
    val createdAt: LocalDateTime,
)
