package com.firstpenguin.app.domain.embedding.usecase

import com.firstpenguin.app.domain.openai.service.OpenAiEmbeddingClient
import org.springframework.stereotype.Component

@Component
class UserQueryEmbeddingProcessor(
    private val openAiEmbeddingClient: OpenAiEmbeddingClient,
) {
    fun embed(query: String): List<Double> = openAiEmbeddingClient.createEmbedding(query)
}
