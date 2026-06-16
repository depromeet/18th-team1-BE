package com.firstpenguin.app.domain.quote.model

import java.time.LocalDateTime

data class ScrappedQuote(
    val quoteId: Long,
    val bookId: Long,
    val bookCoverImageUrl: String,
    val bookPurchaseLink: String,
    val content: String,
    val title: String,
    val author: String,
    val scrappedAt: LocalDateTime,
)
