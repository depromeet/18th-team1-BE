package com.firstpenguin.app.domain.quotebatch.model

import com.firstpenguin.app.global.enums.BatchJobStatus
import java.time.LocalDateTime

data class QuoteBatchJob(
    val id: Long,
    val openAiBatchId: String?,
    val inputFileId: String?,
    val outputFileId: String?,
    val errorFileId: String?,
    val status: BatchJobStatus,
    val jobType: QuoteBatchType,
    val model: String,
    val version: Int,
    val submittedCount: Int,
    val succeededCount: Int,
    val failedCount: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val completedAt: LocalDateTime?,
)
