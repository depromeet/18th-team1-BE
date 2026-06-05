package com.firstpenguin.app.domain.quotemetadata.dto

import com.firstpenguin.app.global.enums.BatchJobStatus

data class ActiveJobStatusResponse(
    val jobId: Long,
    val openAiBatchId: String?,
    val submittedCount: Int,
    val status: BatchJobStatus,
)
