package com.firstpenguin.app.domain.quotemetadata.dto

data class QuoteMetadataBatchStatusResponse(
    val totalQuoteCount: Int,
    val createdCount: Int,
    val pendingCount: Int,
    val processingCount: Int,
    val failedCount: Int,
    val runningJobCount: Int,
    val activeJob: ActiveJobStatusResponse?,
)
