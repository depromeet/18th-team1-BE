package com.firstpenguin.app.domain.embedding.dto.ai

data class OpenAiEmbeddingData(
    val index: Int,
    val embedding: List<Double>,
)
