package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.book.service.BookService
import com.firstpenguin.app.domain.emotion.dto.TagDto
import com.firstpenguin.app.domain.emotion.service.EmotionService
import com.firstpenguin.app.domain.quote.dto.QuoteResponse
import com.firstpenguin.app.domain.quote.model.Quote
import com.firstpenguin.app.domain.quote.service.QuoteService
import com.firstpenguin.app.domain.recommendation.dto.RecommendationDetailResponse
import com.firstpenguin.app.domain.recommendation.dto.RecommendationResponse
import com.firstpenguin.app.domain.recommendation.model.Recommendation
import com.firstpenguin.app.domain.recommendation.model.RecommendationTag
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Component

@Component
class RecommendationResponseMapper(
    private val bookService: BookService,
    private val emotionService: EmotionService,
    private val quoteService: QuoteService,
    private val recommendationService: RecommendationService,
) {
    fun toRecommendationResponse(recommendation: Recommendation): RecommendationResponse {
        val quote = quoteService.findQuoteById(firstRecommendedQuoteId(recommendation.id))

        return toRecommendationResponse(
            recommendationId = recommendation.id,
            quote = quote,
        )
    }

    fun toRecommendationResponse(
        recommendationId: Long,
        quote: Quote,
    ): RecommendationResponse =
        RecommendationResponse(
            recommendationId = recommendationId,
            quote = toQuoteResponse(quote),
        )

    fun toRecommendationDetailResponse(recommendation: Recommendation): RecommendationDetailResponse {
        val quoteId = recommendation.quoteId ?: throw CustomException(ErrorCode.RECOMMENDATION_NOT_COMPLETED)
        val quote = quoteService.findQuoteById(quoteId)
        val (emotionTags, needTag) = toTagDtos(recommendationService.getRecommendationTags(recommendation.id))

        return RecommendationDetailResponse(
            recommendationId = recommendation.id,
            quote = toQuoteResponse(quote),
            emotionValue = recommendation.emotionValue,
            emotionTags = emotionTags,
            needTag = needTag,
            feelingText = recommendation.feelingText,
            diaryText = recommendation.diaryText,
            recommendationDate = recommendation.recommendationDate,
        )
    }

    fun toQuoteResponse(quote: Quote): QuoteResponse {
        val book = bookService.findBookById(quote.bookId)

        return QuoteResponse.from(
            quote = quote,
            book = book,
        )
    }

    private fun firstRecommendedQuoteId(recommendationId: Long): Long =
        recommendationService
            .getRecommendationHistory(recommendationId)
            .firstOrNull()
            ?.quoteId
            ?: throw CustomException(ErrorCode.INVALID_RECOMMENDATION_QUOTE)

    private fun toTagDtos(recommendationTags: List<RecommendationTag>): Pair<List<TagDto>, TagDto?> {
        val tagIds = recommendationTags.map { recommendationTag -> recommendationTag.tagId }
        val (emotionTags, needTag) = emotionService.getEmotionTagsAndNeedTagByIds(tagIds)

        return emotionTags.map(TagDto::from) to needTag?.let(TagDto::from)
    }
}
