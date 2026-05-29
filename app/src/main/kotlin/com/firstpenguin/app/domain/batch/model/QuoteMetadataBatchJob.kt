package com.firstpenguin.app.domain.batch.model

import java.time.LocalDateTime

data class QuoteMetadataBatchJob(
    val id: Long,
    val openAiBatchId: String?,
    val inputFileId: String?,
    val outputFileId: String?,
    val errorFileId: String?,
    val status: String,
    val metadataModel: String,
    val metadataVersion: Int,
    val submittedCount: Int,
    val succeededCount: Int,
    val failedCount: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val completedAt: LocalDateTime?,
)
