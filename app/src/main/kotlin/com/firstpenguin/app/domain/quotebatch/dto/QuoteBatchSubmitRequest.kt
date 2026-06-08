package com.firstpenguin.app.domain.quotebatch.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

private const val MIN_LIMIT = 1L
private const val MAX_LIMIT = 1000L

data class QuoteBatchSubmitRequest(
    @field:Min(MIN_LIMIT)
    @field:Max(MAX_LIMIT)
    val limit: Int,
)
