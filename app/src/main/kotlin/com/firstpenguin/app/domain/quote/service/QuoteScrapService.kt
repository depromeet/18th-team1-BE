package com.firstpenguin.app.domain.quote.service

import com.firstpenguin.app.domain.quote.model.QuoteScrapCursor
import com.firstpenguin.app.domain.quote.model.ScrappedQuote
import com.firstpenguin.app.domain.quote.repository.QuoteScrapRepository
import org.springframework.stereotype.Service

@Service
class QuoteScrapService(
    private val quoteScrapRepository: QuoteScrapRepository,
) {
    fun setQuoteScrap(
        userId: Long,
        quoteId: Long,
    ) {
        quoteScrapRepository.insertIgnoreDuplicate(userId, quoteId)
    }

    fun deleteQuoteScrap(
        userId: Long,
        quoteId: Long,
    ) {
        quoteScrapRepository.deleteByUserIdAndQuoteId(userId, quoteId)
    }

    fun deleteQuoteScraps(
        userId: Long,
        quoteIds: List<Long>,
    ) {
        quoteScrapRepository.deleteByUserIdAndQuoteIds(userId, quoteIds)
    }

    fun countQuoteScraps(userId: Long): Int = quoteScrapRepository.countActiveByUserId(userId)

    fun getScrappedQuotes(
        userId: Long,
        cursor: QuoteScrapCursor?,
        limit: Int,
    ): List<ScrappedQuote> = quoteScrapRepository.findActiveByUserId(userId, cursor, limit)
}
