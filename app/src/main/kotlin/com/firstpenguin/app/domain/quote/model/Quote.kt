package com.firstpenguin.app.domain.quote.model

import java.time.LocalDateTime

data class Quote(
    val id: Long,
    val bookId: Long,
    val content: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime?,
)
