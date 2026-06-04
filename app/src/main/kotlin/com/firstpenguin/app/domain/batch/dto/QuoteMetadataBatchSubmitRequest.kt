package com.firstpenguin.app.domain.batch.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

private const val MIN_LIMIT = 1L
private const val MAX_LIMIT = 1000L

data class QuoteMetadataBatchSubmitRequest(
    @field:Min(MIN_LIMIT)
    @field:Max(MAX_LIMIT)
    val limit: Int,
)
