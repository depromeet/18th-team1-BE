package com.firstpenguin.app.domain.discovery.model

import java.time.LocalDateTime

data class DiscoveryQuote(
    val quoteId: Long,
    val bookId: Long,
    val recommendedUserId: Long,
    val content: String,
    val title: String,
    val author: String,
    val bookCoverImageUrl: String,
    val recommendedAt: LocalDateTime,
    val isScrapped: Boolean,
)
