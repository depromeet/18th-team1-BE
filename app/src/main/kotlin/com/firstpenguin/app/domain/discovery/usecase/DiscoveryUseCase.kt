package com.firstpenguin.app.domain.discovery.usecase

import com.firstpenguin.app.domain.discovery.dto.DiscoveryQuoteResponse
import com.firstpenguin.app.domain.discovery.dto.DiscoveryQuotesResponse
import com.firstpenguin.app.domain.discovery.model.DiscoveryCursor
import com.firstpenguin.app.domain.discovery.model.DiscoveryQuote
import com.firstpenguin.app.domain.discovery.service.DiscoveryService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val DISCOVERY_QUOTE_COUNT = 10
private const val NEXT_PAGE_CHECK_COUNT = 1

@Service
class DiscoveryUseCase(
    private val discoveryService: DiscoveryService,
) {
    @Transactional(readOnly = true)
    fun getDiscoveryQuotes(
        userId: Long,
        cursor: String?,
    ): DiscoveryQuotesResponse {
        val quotes = fetchQuotesWithNextPageCheck(userId, cursor)
        return toDiscoveryQuotesResponse(quotes)
    }

    private fun fetchQuotesWithNextPageCheck(
        userId: Long,
        cursor: String?,
    ): List<DiscoveryQuote> =
        discoveryService.getRecommendedQuotes(
            userId = userId,
            cursor = DiscoveryCursor.parse(cursor),
            limit = DISCOVERY_QUOTE_COUNT + NEXT_PAGE_CHECK_COUNT,
        )

    private fun toDiscoveryQuotesResponse(quotes: List<DiscoveryQuote>): DiscoveryQuotesResponse {
        val pageQuotes = quotes.take(DISCOVERY_QUOTE_COUNT)
        return DiscoveryQuotesResponse(
            quotes = pageQuotes.map(DiscoveryQuoteResponse::from),
            nextCursor = nextCursor(pageQuotes, quotes),
            hasNext = quotes.size > DISCOVERY_QUOTE_COUNT,
        )
    }

    private fun nextCursor(
        pageQuotes: List<DiscoveryQuote>,
        quotes: List<DiscoveryQuote>,
    ): String? {
        if (quotes.size <= DISCOVERY_QUOTE_COUNT) return null

        return pageQuotes.lastOrNull()?.let { quote -> DiscoveryCursor.from(quote).encode() }
    }
}
