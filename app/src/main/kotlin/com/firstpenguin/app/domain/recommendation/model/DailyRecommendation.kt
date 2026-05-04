package com.firstpenguin.app.domain.recommendation.model

import java.time.LocalDate
import java.time.LocalDateTime

data class DailyRecommendation(
    val id: Long,
    val userId: Long,
    val quoteId: Long,
    val recommendationDate: LocalDate,
    val userContext: String?,
    val selectedEmotionRangeId: Long,
    val createdAt: LocalDateTime,
)
