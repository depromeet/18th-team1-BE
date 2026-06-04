package com.firstpenguin.app.global.enums

private const val TEXT_EMBEDDING_3_SMALL_DIMENSION = 1536

enum class QuoteEmbeddingModelVersion(
    val model: String,
    val dimension: Int,
) {
    V1(
        model = "text-embedding-3-small",
        dimension = TEXT_EMBEDDING_3_SMALL_DIMENSION,
    ),
}
