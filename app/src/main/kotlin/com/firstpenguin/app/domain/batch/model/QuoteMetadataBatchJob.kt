package com.firstpenguin.app.domain.batch.model

import java.time.LocalDateTime

data class QuoteMetadataBatchJob(
    val id: Long,
    val openai_batch_id: Long,
    val input_file_id: Long,
    val output_file_id: Long?,
    val error_file_id: Long?,
    val status: String,
    val metadata_model: String,
    val metadata_version: Int,
    val submitted_count: Int,
    val succeeded_count: Int,
    val failed_count: Int,
    val created_at: LocalDateTime,
    val updated_at: LocalDateTime,
    val completed_at: LocalDateTime?,
)
