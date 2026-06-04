package com.firstpenguin.app.domain.embedding.usecase

import com.firstpenguin.app.domain.embedding.model.QuoteEmbedding
import com.firstpenguin.app.domain.embedding.repository.QuoteEmbeddingRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class QuoteEmbeddingCommandUseCase(
    private val quoteEmbeddingRepository: QuoteEmbeddingRepository,
) {
    @Transactional
    fun saveQuoteEmbeddings(quoteEmbeddings: List<QuoteEmbedding>) {
        quoteEmbeddingRepository.upsertQuoteEmbeddings(quoteEmbeddings)
    }
}
