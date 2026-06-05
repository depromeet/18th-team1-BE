package com.firstpenguin.app.domain.openai.dto

data class OpenAiEmbeddingRequest(
    val model: String,
    val input: List<String>,
)
