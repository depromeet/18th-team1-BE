package com.firstpenguin.app.domain.home.useCase

import com.firstpenguin.app.domain.book.service.BookService
import com.firstpenguin.app.domain.diary.service.DiaryService
import com.firstpenguin.app.domain.home.dto.HomeSummaryResponse
import com.firstpenguin.app.domain.home.dto.MonthlyDiaryResponse
import com.firstpenguin.app.domain.home.dto.TodayStatusResponse
import com.firstpenguin.app.domain.quote.dto.QuoteResponse
import com.firstpenguin.app.domain.quote.model.Quote
import com.firstpenguin.app.domain.quote.service.QuoteService
import com.firstpenguin.app.domain.recommendation.service.RecommendationService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

private const val RANDOM_QUOTE_COUNT = 10

@Component
class HomeUseCase(
    private val quoteService: QuoteService,
    private val bookService: BookService,
    private val diaryService: DiaryService,
    private val recommendationService: RecommendationService,
) {
    @Transactional(readOnly = true)
    fun getSummary(userId: Long): HomeSummaryResponse {
        val today = LocalDate.now()
        val monthStart = today.withDayOfMonth(1)
        val monthEnd = today.withDayOfMonth(today.lengthOfMonth())

        val monthlyDiaries = diaryService.findByPeriod(userId = userId, start = monthStart, end = monthEnd)
        val responses = monthlyDiaries.map(MonthlyDiaryResponse::from)
        val todayDiary = responses.firstOrNull { it.createdAt == today }
        val totalDiaryCount = diaryService.countByUserId(userId)

        return HomeSummaryResponse(
            todayDiary = todayDiary,
            monthlyDiaries = responses,
            totalDiaryCount = totalDiaryCount,
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
        val dailyRecommendation = recommendationService.findByUserIdAndRecommendationDate(userId)
        val hasTodayDiary = diaryService.hasTodayDiary(userId)

        return TodayStatusResponse(
            hasTodayRecommendation = dailyRecommendation != null,
            hasTodayDiary = hasTodayDiary,
            dailyRecommendationId =
                dailyRecommendation
                    ?.takeIf { !hasTodayDiary }
                    ?.id,
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
