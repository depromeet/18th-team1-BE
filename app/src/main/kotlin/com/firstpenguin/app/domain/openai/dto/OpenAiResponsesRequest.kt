package com.firstpenguin.app.domain.openai.dto

data class OpenAiResponsesRequest(
    val model: String,
    val reasoning: Map<String, String>,
    val input: String,
    val text: OpenAiResponsesTextRequest,
)

data class OpenAiResponsesTextRequest(
    val format: Map<String, Any>,
    val verbosity: String,
)
