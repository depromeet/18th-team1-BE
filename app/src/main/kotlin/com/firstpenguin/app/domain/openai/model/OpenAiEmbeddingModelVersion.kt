package com.firstpenguin.app.domain.openai.model

private const val TEXT_EMBEDDING_3_SMALL_DIMENSION = 1536

enum class OpenAiEmbeddingModelVersion(
    val model: String,
    val dimension: Int,
) {
    V1(
        model = "text-embedding-3-small",
        dimension = TEXT_EMBEDDING_3_SMALL_DIMENSION,
    ),
}
