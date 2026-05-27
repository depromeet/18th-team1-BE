package com.firstpenguin.app.domain.batch.usecase

import com.firstpenguin.app.domain.batch.service.QuoteMetadataService
import com.firstpenguin.app.domain.quote.model.Quote
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class QuoteMetadataBatchUseCase(
    val quoteMetadataService: QuoteMetadataService
) {
    @Transactional(readOnly = true)
    fun getPendingQuotes(limit: Int) : List<Quote> =
        quoteMetadataService.getPendingQuotes(limit = limit)
}