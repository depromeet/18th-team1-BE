package com.firstpenguin.app.domain.monthlysettlement.model

data class MonthlySettlementEmotionTag(
    val tagId: Long,
    val label: String,
    val count: Int,
    val sortOrder: Int,
)
