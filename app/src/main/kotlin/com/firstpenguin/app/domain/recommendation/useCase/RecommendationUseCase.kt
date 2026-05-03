package com.firstpenguin.app.domain.recommendation.useCase

import com.firstpenguin.app.domain.book.service.BookService
import com.firstpenguin.app.domain.emotion.service.EmotionService
import com.firstpenguin.app.domain.image.service.ImageService
import com.firstpenguin.app.domain.quote.service.QuoteService
import com.firstpenguin.app.domain.recommendation.dto.RecommendationAvailabilityResponse
import com.firstpenguin.app.domain.quote.dto.QuoteResponse
import com.firstpenguin.app.domain.recommendation.dto.RecommendationRequest
import com.firstpenguin.app.domain.recommendation.dto.RecommendationResponse
import com.firstpenguin.app.domain.recommendation.service.RecommendationService
import com.firstpenguin.app.global.enums.ImageOwner
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RecommendationUseCase(
    private val emotionService: EmotionService,
    private val quoteService: QuoteService,
    private val recommendationService: RecommendationService,
    private val bookService: BookService,
    private val imageService: ImageService,
) {
    @Transactional
    fun recommendQuote(userId: Long, request: RecommendationRequest): RecommendationResponse {
        if (recommendationService.hasRecommendedToday(userId)) {
            throw CustomException(ErrorCode.DAILY_RECOMMENDATION_ALREADY_EXISTS)
        }

        val selectEmotionTags = emotionService.selectEmotionTags(request.emotionTagIds)
        val selectToneTags = emotionService.selectToneTags(request.toneTagIds)

        val randomQuote = quoteService.getRandomQuote()

        val selectedEmotionRangeId = selectEmotionTags.first().emotionRangeId
            ?: throw CustomException(ErrorCode.INVALID_EMOTION_TAG)

        recommendationService.createDailyRecommendation(
            userId = userId,
            quoteId = randomQuote.id,
            userContext = request.userContext,
            selectedEmotionRangeId = selectedEmotionRangeId,
        )

        val book = bookService.findBookById(randomQuote.bookId)
        val bookCoverImage = imageService.findUrlsByOwnerIdAndOwnerType(ImageOwner.BOOK, randomQuote.bookId).first()

        val quoteResponse = QuoteResponse(
            quoteId = randomQuote.id,
            bookId = book.id,
            content = randomQuote.content,
            title = book.title,
            author = book.author,
            publisher = book.publisher,
            image = bookCoverImage,
        )

        return RecommendationResponse(quoteResponse, selectEmotionTags, selectToneTags)
    }

    @Transactional(readOnly = true)
    fun isDailyRecommendationAvailable(userId: Long): RecommendationAvailabilityResponse =
        RecommendationAvailabilityResponse(!recommendationService.hasRecommendedToday(userId))
}
