package com.firstpenguin.app.domain.recommendation.useCase

import com.firstpenguin.app.domain.book.service.BookService
import com.firstpenguin.app.domain.emotion.service.EmotionService
import com.firstpenguin.app.domain.image.service.ImageService
import com.firstpenguin.app.domain.quote.dto.QuoteResponse
import com.firstpenguin.app.domain.quote.service.QuoteService
import com.firstpenguin.app.domain.recommendation.dto.RecommendationAvailabilityResponse
import com.firstpenguin.app.domain.recommendation.dto.RecommendationRequest
import com.firstpenguin.app.domain.recommendation.dto.RecommendationResponse
import com.firstpenguin.app.domain.recommendation.service.RecommendationService
import com.firstpenguin.app.global.enums.ImageOwner
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val NEXT_RECOMMENDATION_QUOTE_COUNT = 3

@Service
class RecommendationUseCase(
    private val emotionService: EmotionService,
    private val quoteService: QuoteService,
    private val recommendationService: RecommendationService,
    private val bookService: BookService,
    private val imageService: ImageService,
) {
    @Transactional
    fun recommendQuote(
        userId: Long,
        request: RecommendationRequest,
    ): RecommendationResponse {
        recommendationService.validateRecommendationAvailable(userId)

        val selectEmotionTags = emotionService.selectEmotionTags(request.emotionTagIds)
        val selectToneTags = emotionService.selectToneTags(request.toneTagIds)

        val randomQuote = quoteService.getRandomQuote()

        val selectedEmotionRangeId =
            selectEmotionTags.first().emotionRangeId
                ?: throw CustomException(ErrorCode.INVALID_EMOTION_TAG)

        val userContext = request.userContext.takeIf { it.isNotBlank() }

        val dailyRecommendationId =
            recommendationService.createDailyRecommendation(
                userId = userId,
                quoteId = randomQuote.id,
                userContext = userContext,
                selectedEmotionRangeId = selectedEmotionRangeId,
            )

        recommendationService.createDailyRecommendationQuotes(
            dailyRecommendationId = dailyRecommendationId,
            quoteIds = listOf(randomQuote.id),
        )

        return RecommendationResponse(
            dailyRecommendationId = dailyRecommendationId,
            quote = toQuoteResponse(randomQuote),
            emotionTags = selectEmotionTags,
            toneTags = selectToneTags,
        )
    }

    @Transactional
    fun getNextRecommendationQuotes(userId: Long, dailyRecommendationId: Long): List<QuoteResponse> {
        val dailyRecommendation = recommendationService.getDailyRecommendation(dailyRecommendationId)

        recommendationService.validateOwner(userId, dailyRecommendation)
        recommendationService.validateTodayRecommendation(dailyRecommendation.recommendationDate)

        val recommendationHistory = recommendationService.getRecommendationHistory(dailyRecommendation.id)

        recommendationService.validateRecommendationCount(recommendationHistory.size, NEXT_RECOMMENDATION_QUOTE_COUNT)

        val quoteIdHistory = recommendationHistory.map { recommendationQuote -> recommendationQuote.quoteId }
        val nextQuotes =
            quoteService.getRandomQuotesExcludingIds(
                excludedQuoteIds = quoteIdHistory,
                count = NEXT_RECOMMENDATION_QUOTE_COUNT,
            )

        recommendationService.createDailyRecommendationQuotes(
            dailyRecommendationId = recommendationHistory.first().dailyRecommendationId,
            quoteIds = nextQuotes.map { it.id },
        )

        return nextQuotes.map(::toQuoteResponse)
    }

    @Transactional(readOnly = true)
    fun isDailyRecommendationAvailable(userId: Long): RecommendationAvailabilityResponse =
        RecommendationAvailabilityResponse(!recommendationService.hasRecommendedToday(userId))

    private fun toQuoteResponse(quote: com.firstpenguin.app.domain.quote.model.Quote): QuoteResponse {
        val book = bookService.findBookById(quote.bookId)
        val bookCoverImage =
            imageService
                .findUrlsByOwnerIdAndOwnerType(ImageOwner.BOOK, quote.bookId)
                .firstOrNull()
                ?: throw CustomException(ErrorCode.IMAGE_NOT_FOUND)

        return QuoteResponse(
            quoteId = quote.id,
            bookId = book.id,
            content = quote.content,
            title = book.title,
            author = book.author,
            image = bookCoverImage,
        )
    }
}
