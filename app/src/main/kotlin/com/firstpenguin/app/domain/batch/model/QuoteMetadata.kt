package com.firstpenguin.app.domain.batch.model

import java.time.LocalDateTime

data class QuoteMetadata(
    val id: Long,
    val quote_id: Long,
    val embedding_text: String,
    val metadata_model: String,
    val metadata_version: Int,
    val updated_at: LocalDateTime,
    val completed_at: LocalDateTime?,
)
