package com.firstpenguin.app.domain.batch.model

import java.time.LocalDateTime

data class QuoteMetadataTag(
    val id: Long,
    val quoteMetadataId: Long,
    val tagId: Long,
    val evidence: String?,
    val confidence: Double?,
    val priority: String?,
    val createdAt: LocalDateTime,
)
