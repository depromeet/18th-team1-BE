package com.firstpenguin.app.domain.quotecreation.review.model

import java.time.LocalDateTime

data class QuoteCandidate(
    val id: Long,
    val bookId: Long,
    val content: String,
    val source: String?,
    val status: QuoteCandidateStatus,
    val rejectReason: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
