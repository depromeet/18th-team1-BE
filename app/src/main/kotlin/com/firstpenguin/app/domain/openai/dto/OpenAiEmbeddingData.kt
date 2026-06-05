package com.firstpenguin.app.domain.openai.dto

data class OpenAiEmbeddingData(
    val index: Int,
    val embedding: List<Double>,
)
