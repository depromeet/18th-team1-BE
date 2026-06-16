package com.firstpenguin.app.domain.home.useCase

import com.firstpenguin.app.domain.book.service.BookService
import com.firstpenguin.app.domain.home.dto.HomeSummaryResponse
import com.firstpenguin.app.domain.home.dto.MonthlyRecommendationResponse
import com.firstpenguin.app.domain.home.dto.TodayStatusResponse
import com.firstpenguin.app.domain.quote.dto.QuoteResponse
import com.firstpenguin.app.domain.quote.model.Quote
import com.firstpenguin.app.domain.quote.service.QuoteService
import com.firstpenguin.app.domain.recommendation.model.Recommendation
import com.firstpenguin.app.domain.recommendation.service.RecommendationService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

private const val RANDOM_QUOTE_COUNT = 14

@Component
class HomeUseCase(
    private val quoteService: QuoteService,
    private val bookService: BookService,
    private val recommendationService: RecommendationService,
) {
    @Transactional(readOnly = true)
    fun getSummary(userId: Long): HomeSummaryResponse {
        val today = LocalDate.now()
        val monthStart = today.withDayOfMonth(1)
        val monthEnd = today.withDayOfMonth(today.lengthOfMonth())

        val monthlyRecommendations =
            recommendationService.findCompletedByUserIdAndRecommendationDateBetween(
                userId = userId,
                start = monthStart,
                end = monthEnd,
            )
        val responses = monthlyRecommendations.map(::toMonthlyRecommendationResponse)
        val todayRecommendations = responses.filter { response -> response.createdAt == today }
        val totalRecommendationCount = recommendationService.countCompletedByUserId(userId)

        return HomeSummaryResponse(
            todayRecommendations = todayRecommendations,
            monthlyRecommendations = responses,
            totalRecommendationCount = totalRecommendationCount,
        )
    }

    @Transactional(readOnly = true)
    fun getRandomQuotes(): List<QuoteResponse> {
        val randomQuotes =
            quoteService.getRandomQuotesExcludingIds(
                excludedQuoteIds = emptyList(),
                count = RANDOM_QUOTE_COUNT,
            )

        return randomQuotes.map(::toQuoteResponse)
    }

    @Transactional(readOnly = true)
    fun getTodayStatus(userId: Long): TodayStatusResponse {
        val today = LocalDate.now()
        val todayRecommendations =
            recommendationService.findByUserIdAndRecommendationDateBetween(
                userId = userId,
                start = today,
                end = today,
            )
        val ongoingRecommendation = todayRecommendations.lastOrNull { recommendation -> recommendation.quoteId == null }
        val canCreateTodayRecommendation =
            !recommendationService.hasReachedRecommendationLimit(todayRecommendations.size)

        return TodayStatusResponse(
            hasOngoingRecommendation = ongoingRecommendation != null,
            ongoingRecommendationId = ongoingRecommendation?.id,
            canCreateTodayRecommendation = canCreateTodayRecommendation,
        )
    }

    private fun toMonthlyRecommendationResponse(recommendation: Recommendation): MonthlyRecommendationResponse {
        val quoteId = checkNotNull(recommendation.quoteId)
        val quote = quoteService.findQuoteById(quoteId)

        return MonthlyRecommendationResponse.from(
            recommendation = recommendation,
            quote = quote,
        )
    }

    private fun toQuoteResponse(quote: Quote): QuoteResponse {
        val book = bookService.findBookById(quote.bookId)

        return QuoteResponse.from(
            quote = quote,
            book = book,
        )
    }
}
