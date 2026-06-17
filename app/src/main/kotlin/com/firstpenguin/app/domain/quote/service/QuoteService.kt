package com.firstpenguin.app.domain.quote.service

import com.firstpenguin.app.domain.quote.model.Quote
import com.firstpenguin.app.domain.quote.repository.QuoteRepository
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service

private const val RANDOM_PICK_MAX_ATTEMPTS = 100

@Service
class QuoteService(
    private val quoteRepository: QuoteRepository,
) {
    fun findQuoteById(id: Long): Quote =
        quoteRepository.findQuoteById(id)
            ?: throw CustomException(ErrorCode.QUOTE_NOT_FOUND)

    fun findBookCoverImageUrlsByQuoteIds(quoteIds: List<Long>): Map<Long, String> =
        quoteRepository.findBookCoverImageUrlsByQuoteIds(quoteIds)

    fun getRandomQuote(): Quote = getRandomQuoteExcludingIds(emptyList())

    fun getRandomQuoteExcludingIds(excludedQuoteIds: List<Long>): Quote =
        findRandomQuoteExcludingIds(excludedQuoteIds.toSet())
            ?: throw CustomException(ErrorCode.NOT_ENOUGH_QUOTES)

    fun getRandomQuotesExcludingIds(
        excludedQuoteIds: List<Long>,
        count: Int,
    ): List<Quote> {
        if (count <= 0) return emptyList()

        val quotes = mutableListOf<Quote>()
        val excludedIds = excludedQuoteIds.toMutableSet()

        repeat(RANDOM_PICK_MAX_ATTEMPTS * count) {
            if (quotes.size < count) {
                val quote = findRandomQuoteExcludingIds(excludedIds)
                if (quote != null) {
                    quotes += quote
                    excludedIds += quote.id
                }
            }
        }

        if (quotes.size == count) return quotes

        throw CustomException(ErrorCode.NOT_ENOUGH_QUOTES)
    }

    private fun findRandomQuoteExcludingIds(excludedQuoteIds: Set<Long>): Quote? {
        val maxQuoteId = quoteRepository.getMaxQuoteId()
        if (maxQuoteId == 0L) return null

        var foundQuote: Quote? = null

        repeat(RANDOM_PICK_MAX_ATTEMPTS) {
            if (foundQuote == null) {
                val randomId = (1..maxQuoteId).random()

                if (randomId !in excludedQuoteIds) {
                    val quote = quoteRepository.findQuoteById(randomId)
                    if (quote != null && quote.deletedAt == null) {
                        foundQuote = quote
                    }
                }
            }
        }

        return foundQuote
    }
}
