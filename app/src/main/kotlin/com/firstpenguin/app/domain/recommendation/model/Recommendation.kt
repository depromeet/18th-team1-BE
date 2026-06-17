package com.firstpenguin.app.domain.recommendation.model

import java.time.LocalDate
import java.time.LocalDateTime

data class Recommendation(
    val id: Long,
    val userId: Long,
    val quoteId: Long?,
    val recommendationDate: LocalDate,
    val feelingText: String?,
    val diaryText: String?,
    val emotionRangeId: Long,
    val createdAt: LocalDateTime,
)
