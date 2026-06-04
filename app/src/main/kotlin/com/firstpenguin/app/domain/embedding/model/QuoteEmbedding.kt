package com.firstpenguin.app.domain.embedding.model

data class QuoteEmbedding(
    val quoteId: Long,
    val embeddingModel: String,
    val embedding: List<Double>,
    val embeddingTextHash: String,
)
