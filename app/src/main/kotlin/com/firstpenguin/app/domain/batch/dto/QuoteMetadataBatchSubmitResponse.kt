package com.firstpenguin.app.domain.batch.dto

data class QuoteMetadataBatchSubmitResponse(
    val jobId: Long,
    val openAiBatchId: String,
)
