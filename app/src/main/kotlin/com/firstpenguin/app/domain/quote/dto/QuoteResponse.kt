package com.firstpenguin.app.domain.quote.dto

data class QuoteResponse(
    val quoteId: Long,
    val bookId: Long,
    val content: String,
    val title: String,
    val author: String,
    val image: String,
)
