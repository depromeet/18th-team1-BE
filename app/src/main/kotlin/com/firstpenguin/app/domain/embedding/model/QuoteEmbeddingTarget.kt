package com.firstpenguin.app.domain.embedding.model

data class QuoteEmbeddingTarget(
    val quoteId: Long,
    val embeddingText: String,
    val existingEmbeddingTextHash: String?,
)
