package com.firstpenguin.app.domain.quote.service

import com.firstpenguin.app.domain.quote.model.Quote
import com.firstpenguin.app.domain.quote.repository.QuoteRepository
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service

@Service
class QuoteService(
    private val quoteRepository: QuoteRepository,
) {
    fun getRandomQuote(): Quote =
        quoteRepository.findRandomQuote()
            ?: throw CustomException(ErrorCode.QUOTE_NOT_FOUND)
}
