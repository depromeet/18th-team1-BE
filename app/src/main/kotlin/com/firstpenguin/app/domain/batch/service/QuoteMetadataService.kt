package com.firstpenguin.app.domain.batch.service

import com.firstpenguin.app.domain.batch.repository.QuoteMetadataRepository
import com.firstpenguin.app.domain.quote.model.Quote
import org.springframework.stereotype.Service

@Service
class QuoteMetadataService(
    private val quoteMetadataRepository: QuoteMetadataRepository,
) {
    fun getPendingQuotes(limit: Int) : List<Quote> =
        quoteMetadataRepository.findPendingQuotes(limit = limit)
}
