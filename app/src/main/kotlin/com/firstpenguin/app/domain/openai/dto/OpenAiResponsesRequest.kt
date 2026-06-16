package com.firstpenguin.app.domain.openai.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class OpenAiResponsesRequest(
    val model: String,
    val reasoning: Map<String, String>,
    val input: String,
    val text: OpenAiResponsesTextRequest,
    @get:JsonProperty("prompt_cache_key")
    val promptCacheKey: String? = null,
)

data class OpenAiResponsesTextRequest(
    val format: Map<String, Any>,
    val verbosity: String,
)

data class OpenAiTextResponse(
    val outputText: String,
    val inputTokens: Long?,
    val cachedTokens: Long?,
    val outputTokens: Long?,
)
