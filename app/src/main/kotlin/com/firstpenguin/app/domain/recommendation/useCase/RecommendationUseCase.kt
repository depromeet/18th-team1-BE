package com.firstpenguin.app.domain.recommendation.useCase

import com.firstpenguin.app.domain.book.service.BookService
import com.firstpenguin.app.domain.emotion.dto.TagDto
import com.firstpenguin.app.domain.emotion.service.EmotionService
import com.firstpenguin.app.domain.quote.dto.QuoteResponse
import com.firstpenguin.app.domain.quote.model.Quote
import com.firstpenguin.app.domain.quote.service.QuoteService
import com.firstpenguin.app.domain.recommendation.dto.DailyRecommendationResponse
import com.firstpenguin.app.domain.recommendation.dto.RecommendationRequest
import com.firstpenguin.app.domain.recommendation.dto.RecommendationResponse
import com.firstpenguin.app.domain.recommendation.model.DailyRecommendationTag
import com.firstpenguin.app.domain.recommendation.service.RecommendationService
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
) {
    @Transactional
    fun recommendQuote(
        userId: Long,
        request: RecommendationRequest,
    ): RecommendationResponse {
        recommendationService.validateRecommendationAvailable(userId)

        val selectedEmotionTags = emotionService.selectEmotionTags(request.emotionTagIds)
        emotionService.selectToneTags(request.toneTagIds)
        val selectedEmotionRangeId =
            selectedEmotionTags.first().emotionRangeId
                ?: throw CustomException(ErrorCode.INVALID_EMOTION_TAG)

        val randomQuote = quoteService.getRandomQuote()

        val dailyRecommendationId =
            recommendationService.createDailyRecommendation(
                userId = userId,
                quoteId = randomQuote.id,
                userContext = request.userContext.takeIf { it.isNotBlank() },
                selectedEmotionRangeId = selectedEmotionRangeId,
            )

        recommendationService.createDailyRecommendationQuotes(
            dailyRecommendationId = dailyRecommendationId,
            quoteIds = listOf(randomQuote.id),
        )

        recommendationService.createDailyRecommendationTags(
            dailyRecommendationId = dailyRecommendationId,
            tagIds = request.emotionTagIds + request.toneTagIds,
        )

        return RecommendationResponse(
            dailyRecommendationId = dailyRecommendationId,
            quote = toQuoteResponse(randomQuote),
        )
    }

    @Transactional(readOnly = true)
    fun getDailyRecommendationDetail(
        userId: Long,
        dailyRecommendationId: Long,
    ): DailyRecommendationResponse {
        val dailyRecommendation =
            recommendationService.validateDailyRecommendation(
                userId = userId,
                dailyRecommendationId = dailyRecommendationId,
            )

        val quote = quoteService.findQuoteById(dailyRecommendation.quoteId)
        val recommendationTags = recommendationService.getRecommendationTags(dailyRecommendationId)
        val (emotionTags, toneTags) = toTagDtos(recommendationTags)

        return DailyRecommendationResponse(
            dailyRecommendationId = dailyRecommendationId,
            quote = toQuoteResponse(quote),
            emotionTags = emotionTags,
            toneTags = toneTags,
        )
    }

    @Transactional
    fun getNextRecommendationQuotes(
        userId: Long,
        dailyRecommendationId: Long,
    ): List<QuoteResponse> {
        recommendationService.validateDailyRecommendation(userId, dailyRecommendationId)

        val recommendationHistory = recommendationService.getRecommendationHistory(dailyRecommendationId)

        recommendationService.validateRecommendationCount(
            currentCount = recommendationHistory.size,
            nextCount = NEXT_RECOMMENDATION_QUOTE_COUNT,
        )

        val quoteIdHistory = recommendationHistory.map { recommendationQuote -> recommendationQuote.quoteId }
        val nextQuotes =
            quoteService.getRandomQuotesExcludingIds(
                excludedQuoteIds = quoteIdHistory,
                count = NEXT_RECOMMENDATION_QUOTE_COUNT,
            )

        recommendationService.createDailyRecommendationQuotes(
            dailyRecommendationId = dailyRecommendationId,
            quoteIds = nextQuotes.map { it.id },
        )

        return nextQuotes.map(::toQuoteResponse)
    }

    private fun toQuoteResponse(quote: Quote): QuoteResponse {
        val book = bookService.findBookById(quote.bookId)

        return QuoteResponse.from(
            quote = quote,
            book = book,
        )
    }

    private fun toTagDtos(recommendationTags: List<DailyRecommendationTag>): Pair<List<TagDto>, List<TagDto>> {
        val tagIds = recommendationTags.map { recommendationTag -> recommendationTag.tagId }
        val (emotionTags, toneTags) = emotionService.getTagsByIds(tagIds)

        return emotionTags.map(TagDto::from) to toneTags.map(TagDto::from)
    }
}
