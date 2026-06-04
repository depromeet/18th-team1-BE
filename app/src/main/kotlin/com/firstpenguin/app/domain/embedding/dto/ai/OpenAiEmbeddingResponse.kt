package com.firstpenguin.app.domain.embedding.dto.ai

import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode

data class OpenAiEmbeddingResponse(
    val data: List<OpenAiEmbeddingData>,
) {
    fun toEmbeddings(): List<List<Double>> =
        data
            .takeIf { items -> items.isNotEmpty() }
            ?.sortedBy { item -> item.index }
            ?.map { item -> item.embedding }
            ?: throw CustomException(ErrorCode.QUOTE_EMBEDDING_RESPONSE_EMPTY)
}
