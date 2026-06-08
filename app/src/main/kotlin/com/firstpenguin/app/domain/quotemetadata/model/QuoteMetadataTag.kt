package com.firstpenguin.app.domain.quotemetadata.model

import java.time.LocalDateTime

data class QuoteMetadataTag(
    val id: Long? = null,
    val quoteMetadataId: Long,
    val tagId: Long,
    val createdAt: LocalDateTime? = null,
)
