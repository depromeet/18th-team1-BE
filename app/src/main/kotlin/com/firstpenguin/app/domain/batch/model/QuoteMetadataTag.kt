package com.firstpenguin.app.domain.batch.model

import java.time.LocalDateTime

data class QuoteMetadataTag(
    val id: Long,
    val quoteMetadataId: Long,
    val tagId: Long,
    val createdAt: LocalDateTime,
)
