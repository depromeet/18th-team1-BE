package com.firstpenguin.app.domain.quote.service

import com.firstpenguin.app.domain.quote.model.Quote
import com.firstpenguin.app.domain.quote.repository.QuoteRepository
import org.springframework.stereotype.Service

@Service
class QuoteService(
    private val quoteRepository: QuoteRepository,
) {
    fun getRandomQuote(): Quote {
        while (true) {
            val maxQuoteId = quoteRepository.getMaxQuoteId()
            val randomId = (1..maxQuoteId).random()

            val quote = quoteRepository.findQuoteById(randomId)

            if (quote != null) {
                return quote
            }
        }
    }
}
