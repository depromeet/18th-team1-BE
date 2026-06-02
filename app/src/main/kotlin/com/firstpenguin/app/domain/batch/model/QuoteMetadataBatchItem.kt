package com.firstpenguin.app.domain.batch.model

import com.firstpenguin.app.global.enums.BatchItemStatus
import java.time.LocalDateTime

data class QuoteMetadataBatchItem(
    val id: Long,
    val jobId: Long,
    val quoteId: Long,
    val status: BatchItemStatus,
    val errorMessage: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
