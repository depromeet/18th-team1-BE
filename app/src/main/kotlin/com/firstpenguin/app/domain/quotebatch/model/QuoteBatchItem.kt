package com.firstpenguin.app.domain.quotebatch.model

import com.firstpenguin.app.global.enums.BatchItemStatus
import java.time.LocalDateTime

data class QuoteBatchItem(
    val id: Long,
    val jobId: Long,
    val jobType: QuoteBatchType,
    val targetId: Long,
    val customId: String,
    val status: BatchItemStatus,
    val errorMessage: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
