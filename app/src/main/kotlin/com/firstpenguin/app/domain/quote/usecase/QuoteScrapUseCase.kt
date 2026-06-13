package com.firstpenguin.app.domain.quote.usecase

import com.firstpenguin.app.domain.quote.dto.ScrappedQuoteResponse
import com.firstpenguin.app.domain.quote.dto.ScrappedQuotesResponse
import com.firstpenguin.app.domain.quote.model.QuoteScrapCursor
import com.firstpenguin.app.domain.quote.model.ScrappedQuote
import com.firstpenguin.app.domain.quote.service.QuoteScrapService
import com.firstpenguin.app.domain.quote.service.QuoteService
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val DEFAULT_SCRAPPED_QUOTE_COUNT = 10
private const val NEXT_PAGE_CHECK_COUNT = 1
private const val MAX_SCRAPPED_QUOTE_COUNT = 50

@Service
class QuoteScrapUseCase(
    private val quoteService: QuoteService,
    private val quoteScrapService: QuoteScrapService,
) {
    @Transactional
    fun setQuoteScrap(
        userId: Long,
        quoteId: Long,
    ) {
        validateQuoteExists(quoteId)
        quoteScrapService.setQuoteScrap(userId, quoteId)
    }

    @Transactional
    fun deleteQuoteScrap(
        userId: Long,
        quoteId: Long,
    ) {
        validateQuoteExists(quoteId)
        quoteScrapService.deleteQuoteScrap(userId, quoteId)
    }

    @Transactional
    fun deleteQuoteScraps(
        userId: Long,
        quoteIds: List<Long>,
    ) {
        validateQuoteIds(quoteIds)
        quoteScrapService.deleteQuoteScraps(userId, quoteIds.distinct())
    }

    @Transactional(readOnly = true)
    fun getScrappedQuotes(
        userId: Long,
        cursor: String?,
        limit: Int = DEFAULT_SCRAPPED_QUOTE_COUNT,
    ): ScrappedQuotesResponse {
        validateLimit(limit)
        val quotes = fetchQuotesWithNextPageCheck(userId, cursor, limit)
        return toScrappedQuotesResponse(userId, limit, quotes)
    }

    private fun fetchQuotesWithNextPageCheck(
        userId: Long,
        cursor: String?,
        limit: Int,
    ): List<ScrappedQuote> =
        quoteScrapService.getScrappedQuotes(
            userId = userId,
            cursor = QuoteScrapCursor.parse(cursor),
            limit = limit + NEXT_PAGE_CHECK_COUNT,
        )

    private fun toScrappedQuotesResponse(
        userId: Long,
        limit: Int,
        quotes: List<ScrappedQuote>,
    ): ScrappedQuotesResponse {
        val pageQuotes = quotes.take(limit)
        return ScrappedQuotesResponse(
            totalCount = quoteScrapService.countQuoteScraps(userId),
            quotes = pageQuotes.map(ScrappedQuoteResponse::from),
            nextCursor = nextCursor(pageQuotes, quotes, limit),
            hasNext = quotes.size > limit,
        )
    }

    private fun nextCursor(
        pageQuotes: List<ScrappedQuote>,
        quotes: List<ScrappedQuote>,
        limit: Int,
    ): String? {
        if (quotes.size <= limit) return null

        return pageQuotes.lastOrNull()?.let { quote -> QuoteScrapCursor.from(quote).encode() }
    }

    private fun validateLimit(limit: Int) {
        if (limit !in MIN_SCRAPPED_QUOTE_COUNT..MAX_SCRAPPED_QUOTE_COUNT) {
            throw CustomException(ErrorCode.INVALID_INPUT)
        }
    }

    private fun validateQuoteIds(quoteIds: List<Long>) {
        if (quoteIds.isEmpty() || quoteIds.size > MAX_SCRAPPED_QUOTE_COUNT) {
            throw CustomException(ErrorCode.INVALID_INPUT)
        }

        if (quoteIds.any { quoteId -> quoteId <= 0 }) {
            throw CustomException(ErrorCode.INVALID_INPUT)
        }
    }

    private fun validateQuoteExists(quoteId: Long) {
        try {
            quoteService.findQuoteById(quoteId)
        } catch (exception: CustomException) {
            if (exception.errorCode == ErrorCode.NOT_FOUND) {
                throw CustomException(ErrorCode.QUOTE_NOT_FOUND)
            }
            throw exception
        }
    }

    private companion object {
        const val MIN_SCRAPPED_QUOTE_COUNT = 1
    }
}
