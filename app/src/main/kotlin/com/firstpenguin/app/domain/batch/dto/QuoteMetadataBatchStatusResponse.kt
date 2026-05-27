package com.firstpenguin.app.domain.batch.dto

data class QuoteMetadataBatchStatusResponse(
    val totalQuoteCount: Int,
    val createdCount: Int,
    val pendingCount: Int,
    val progressCount: Int,
    val failedCount: Int,
    val runningJobCount: Int,
    val activeJob: ActiveJobStatusResponse,
)
