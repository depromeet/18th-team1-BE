package com.firstpenguin.app.domain.batch.dto

data class ActiveJobStatusResponse(
    val jobId: Long,
    val openAiBatchId: String,
    val submittedCount: Int,
    val status: String,
)
