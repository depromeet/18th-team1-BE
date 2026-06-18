package com.firstpenguin.app.domain.monthlysettlement.model

data class MonthlySettlementSelectedBook(
    val quoteId: Long,
    val bookId: Long,
    val quoteContent: String,
    val title: String,
    val author: String,
    val bookCoverImageUrl: String,
    val genre: String,
    val bookPurchaseLink: String,
)
