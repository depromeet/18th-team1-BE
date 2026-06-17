package com.firstpenguin.app.domain.monthlysettlement.model

data class MonthlySettlementSnapshot(
    val settlement: MonthlySettlement,
    val monthlyBooks: List<MonthlySettlementBook>,
    val emotionTags: List<MonthlySettlementEmotionTag>,
)
