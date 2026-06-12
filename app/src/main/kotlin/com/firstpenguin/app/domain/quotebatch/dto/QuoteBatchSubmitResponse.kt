package com.firstpenguin.app.domain.quotebatch.dto

data class QuoteBatchSubmitResponse(
    val jobId: Long,
    val openAiBatchId: String,
)
