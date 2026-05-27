package com.firstpenguin.app.domain.batch.dto

import jakarta.validation.constraints.Min

private const val MAX_LIMIT: Long = 1000L

data class QuoteMetadataBatchSubmitRequest(
    @field:Min(MAX_LIMIT)
    val limit: Long,
)
