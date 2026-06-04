package com.firstpenguin.app.domain.embedding.dto.ai

data class OpenAiEmbeddingRequest(
    val model: String,
    val input: List<String>,
)
