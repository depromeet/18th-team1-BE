package com.firstpenguin.app.domain.quotemetadata.dto

data class QuoteMetadataBatchSubmitResponse(
    val jobId: Long,
    val openAiBatchId: String,
)
