package com.firstpenguin.app.domain.batch.model

import java.time.LocalDateTime

data class QuoteMetadataTag(
    val id: Long,
    val quote_metadata_id: Long,
    val tag_id: Long,
    val created_at: LocalDateTime,
)
