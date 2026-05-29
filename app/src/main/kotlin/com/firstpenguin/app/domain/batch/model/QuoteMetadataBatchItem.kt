package com.firstpenguin.app.domain.batch.model

import java.time.LocalDateTime

data class QuoteMetadataBatchItem(
    val id: Long,
    val jobId: Long,
    val quoteId: Long,
    val status: String,
    val errorMessage: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
