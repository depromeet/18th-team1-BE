package com.firstpenguin.app.domain.quotecreation.dto

data class QuoteCreationBatchStatusResponse(
    val totalBookCount: Int,
    val extractedBookCount: Int,
    val pendingBookCount: Int,
    val processingBookCount: Int,
    val failedBookCount: Int,
    val runningJobCount: Int,
    val activeJob: QuoteCreationBatchActiveJobStatusResponse?,
)
