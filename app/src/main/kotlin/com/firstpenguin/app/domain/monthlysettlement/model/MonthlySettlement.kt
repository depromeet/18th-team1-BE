package com.firstpenguin.app.domain.monthlysettlement.model

import java.time.LocalDateTime

data class MonthlySettlement(
    val id: Long,
    val userId: Long,
    val year: Int,
    val month: Int,
    val sharedQuoteCount: Int,
    val mostFrequentGenre: String?,
    val topEmotionTagId: Long,
    val topEmotionTagLabel: String,
    val recommendationMessage: String,
    val monthlyBook: MonthlySettlementSelectedBook?,
    val createdAt: LocalDateTime,
)
