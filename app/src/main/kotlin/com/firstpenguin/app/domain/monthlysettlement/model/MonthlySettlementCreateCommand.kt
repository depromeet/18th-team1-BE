package com.firstpenguin.app.domain.monthlysettlement.model

data class MonthlySettlementCreateCommand(
    val userId: Long,
    val year: Int,
    val month: Int,
    val sharedQuoteCount: Int,
    val mostFrequentGenre: String?,
    val topEmotionTag: MonthlySettlementEmotionTag,
    val recommendationMessage: String,
    val monthlyBook: MonthlySettlementSelectedBook?,
    val monthlyBooks: List<MonthlySettlementBook>,
    val emotionTags: List<MonthlySettlementEmotionTag>,
)
