package com.firstpenguin.app.domain.batch.model

import java.time.LocalDateTime

data class QuoteMetadataBatchItem(
    val id: Long,
    val job_id: Long,
    val quote_id: Long,
    val status: String,
    val error_message: String?,
    val created_at: LocalDateTime,
    val updated_at: LocalDateTime,
)
