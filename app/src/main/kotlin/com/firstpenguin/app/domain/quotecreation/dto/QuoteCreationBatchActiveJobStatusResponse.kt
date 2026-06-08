package com.firstpenguin.app.domain.quotecreation.dto

import com.firstpenguin.app.global.enums.BatchJobStatus

data class QuoteCreationBatchActiveJobStatusResponse(
    val jobId: Long,
    val openAiBatchId: String?,
    val submittedCount: Int,
    val status: BatchJobStatus,
)
