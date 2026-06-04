package com.firstpenguin.app.domain.batch.model

import java.time.LocalDateTime

data class QuoteMetadata(
    val id: Long? = null,
    val quoteId: Long,
    val embeddingText: String,
    val metadataModel: String,
    val metadataVersion: Int,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
)
