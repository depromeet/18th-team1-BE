package com.firstpenguin.app.domain.discovery.usecase

import com.firstpenguin.app.domain.discovery.dto.DiscoveryQuoteResponse
import com.firstpenguin.app.domain.discovery.dto.DiscoveryQuotesResponse
import com.firstpenguin.app.domain.discovery.model.DiscoveryCursor
import com.firstpenguin.app.domain.discovery.model.DiscoveryQuote
import com.firstpenguin.app.domain.discovery.model.DiscoveryQuoteSearchCriteria
import com.firstpenguin.app.domain.discovery.model.DiscoveryQuoteSearchCursor
import com.firstpenguin.app.domain.discovery.model.DiscoveryQuoteSearchSort
import com.firstpenguin.app.domain.discovery.service.DiscoveryService
import com.firstpenguin.app.domain.genre.service.GenreService
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val DISCOVERY_QUOTE_COUNT = 10
private const val NEXT_PAGE_CHECK_COUNT = 1

@Service
class DiscoveryUseCase(
    private val discoveryService: DiscoveryService,
    private val genreService: GenreService,
) {
    @Transactional(readOnly = true)
    fun getDiscoveryQuotes(
        userId: Long,
        cursor: String?,
        genreId: Long?,
    ): DiscoveryQuotesResponse {
        validateGenreId(genreId)
        val quotes = fetchQuotesWithNextPageCheck(userId, cursor, genreId)
        return toDiscoveryQuotesResponse(quotes) { quote -> DiscoveryCursor.from(quote).encode() }
    }

    @Transactional(readOnly = true)
    fun searchDiscoveryQuotes(
        userId: Long,
        query: String?,
        sort: String?,
        cursor: String?,
        genreId: Long?,
    ): DiscoveryQuotesResponse {
        val searchSort = DiscoveryQuoteSearchSort.parse(sort)
        validateGenreId(genreId)
        val quotes = fetchSearchQuotesWithNextPageCheck(userId, query, searchSort, cursor, genreId)
        return toDiscoveryQuotesResponse(quotes) { quote ->
            DiscoveryQuoteSearchCursor
                .from(quote, searchSort)
                .encode(searchSort)
        }
    }

    private fun fetchQuotesWithNextPageCheck(
        userId: Long,
        cursor: String?,
        genreId: Long?,
    ): List<DiscoveryQuote> =
        discoveryService.getRecommendedQuotes(
            userId = userId,
            cursor = DiscoveryCursor.parse(cursor),
            genreId = genreId,
            limit = DISCOVERY_QUOTE_COUNT + NEXT_PAGE_CHECK_COUNT,
        )

    private fun fetchSearchQuotesWithNextPageCheck(
        userId: Long,
        query: String?,
        sort: DiscoveryQuoteSearchSort,
        cursor: String?,
        genreId: Long?,
    ): List<DiscoveryQuote> =
        discoveryService.searchRecommendedQuotes(
            DiscoveryQuoteSearchCriteria(
                userId = userId,
                query = normalizeQuery(query),
                sort = sort,
                cursor = DiscoveryQuoteSearchCursor.parse(cursor, sort),
                genreId = genreId,
                limit = DISCOVERY_QUOTE_COUNT + NEXT_PAGE_CHECK_COUNT,
            ),
        )

    private fun normalizeQuery(query: String?): String =
        query?.trim()?.takeIf { text -> text.isNotBlank() }
            ?: throw CustomException(ErrorCode.INVALID_INPUT)

    private fun validateGenreId(genreId: Long?) {
        if (genreId == null) return
        if (genreService.existsGenre(genreId)) return

        throw CustomException(ErrorCode.INVALID_INPUT)
    }

    private fun toDiscoveryQuotesResponse(
        quotes: List<DiscoveryQuote>,
        cursorEncoder: (DiscoveryQuote) -> String,
    ): DiscoveryQuotesResponse {
        val pageQuotes = quotes.take(DISCOVERY_QUOTE_COUNT)
        return DiscoveryQuotesResponse(
            quotes = pageQuotes.map(DiscoveryQuoteResponse::from),
            nextCursor = nextCursor(pageQuotes, quotes, cursorEncoder),
            hasNext = quotes.size > DISCOVERY_QUOTE_COUNT,
        )
    }

    private fun nextCursor(
        pageQuotes: List<DiscoveryQuote>,
        quotes: List<DiscoveryQuote>,
        cursorEncoder: (DiscoveryQuote) -> String,
    ): String? {
        if (quotes.size <= DISCOVERY_QUOTE_COUNT) return null

        return pageQuotes.lastOrNull()?.let(cursorEncoder)
    }
}
