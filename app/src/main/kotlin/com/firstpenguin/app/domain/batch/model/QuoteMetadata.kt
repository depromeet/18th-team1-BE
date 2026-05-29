package com.firstpenguin.app.domain.batch.model

import java.time.LocalDateTime

data class QuoteMetadata(
    val id: Long,
    val quoteId: Long,
    val embeddingText: String,
    val metadataModel: String,
    val metadataVersion: Int,
    val updatedAt: LocalDateTime,
    val completedAt: LocalDateTime?,
)
