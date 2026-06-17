package com.firstpenguin.app.domain.recommendation.model

import com.firstpenguin.app.global.enums.EmotionRangeName
import java.time.LocalDate
import java.time.LocalDateTime

data class Recommendation(
    val id: Long,
    val userId: Long,
    val quoteId: Long?,
    val recommendationDate: LocalDate,
    val feelingText: String?,
    val diaryText: String?,
    val emotionValue: Int,
    val emotionRangeId: Long,
    val emotionRangeName: EmotionRangeName,
    val createdAt: LocalDateTime,
)
