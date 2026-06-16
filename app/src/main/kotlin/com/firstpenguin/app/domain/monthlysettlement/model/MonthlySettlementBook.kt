package com.firstpenguin.app.domain.monthlysettlement.model

data class MonthlySettlementBook(
    val bookId: Long,
    val title: String,
    val author: String,
    val bookCoverImageUrl: String,
    val genre: String,
    val sortOrder: Int,
)
