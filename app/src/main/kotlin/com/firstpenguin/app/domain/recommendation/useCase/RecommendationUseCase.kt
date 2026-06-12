package com.firstpenguin.app.domain.recommendation.useCase

import com.firstpenguin.app.domain.book.service.BookService
import com.firstpenguin.app.domain.emotion.dto.TagDto
import com.firstpenguin.app.domain.emotion.service.EmotionService
import com.firstpenguin.app.domain.quote.dto.QuoteResponse
import com.firstpenguin.app.domain.quote.model.Quote
import com.firstpenguin.app.domain.quote.service.QuoteService
import com.firstpenguin.app.domain.recommendation.dto.RecommendationDetailResponse
import com.firstpenguin.app.domain.recommendation.dto.RecommendationPeriodItemResponse
import com.firstpenguin.app.domain.recommendation.dto.RecommendationPeriodResponse
import com.firstpenguin.app.domain.recommendation.dto.RecommendationRequest
import com.firstpenguin.app.domain.recommendation.dto.RecommendationResponse
import com.firstpenguin.app.domain.recommendation.model.Recommendation
import com.firstpenguin.app.domain.recommendation.model.RecommendationTag
import com.firstpenguin.app.domain.recommendation.service.RecommendationService
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

private const val NEXT_RECOMMENDATION_QUOTE_COUNT = 9
private const val MAX_EMOTION_TAG_COUNT = 5

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
        validateRecommendationRequest(request)

        val selectedTagIds = request.emotionTagIds + listOfNotNull(request.needTagId)

        emotionService.validateTags(
            emotionRangeId = request.emotionRangeId,
            emotionTagIds = request.emotionTagIds,
            needTagId = request.needTagId,
        )

        recommendationService.lockRecommendationCreation(userId)
        recommendationService.findOngoingRecommendation(userId)?.let { recommendation ->
            return toRecommendationResponse(recommendation)
        }

        recommendationService.validateRecommendationCreatable(userId)

        val recommendationId =
            recommendationService.createRecommendation(
                userId = userId,
                feelingText = request.feelingText.normalizedText(),
                diaryText = request.diaryText,
                emotionRangeId = request.emotionRangeId,
            )
        val randomQuote = quoteService.getRandomQuote()

        recommendationService.createRecommendationTags(
            recommendationId = recommendationId,
            tagIds = selectedTagIds,
        )
        recommendationService.createRecommendationQuotes(
            recommendationId = recommendationId,
            quoteIds = listOf(randomQuote.id),
        )

        return toRecommendationResponse(
            recommendationId = recommendationId,
            quote = randomQuote,
        )
    }

    private fun validateRecommendationRequest(request: RecommendationRequest) {
        validateEmotionTagCount(request.emotionTagIds)
        validateNeedInput(request)
    }

    private fun validateEmotionTagCount(emotionTagIds: List<Long>) {
        if (emotionTagIds.isEmpty() || emotionTagIds.size > MAX_EMOTION_TAG_COUNT) {
            throw CustomException(ErrorCode.INVALID_EMOTION_TAG)
        }
    }

    private fun validateNeedInput(request: RecommendationRequest) {
        val hasNeedTag = request.needTagId != null
        val hasFeelingText = request.feelingText.hasText()

        if (hasNeedTag == hasFeelingText) {
            throw CustomException(ErrorCode.INVALID_RECOMMENDATION_NEED_INPUT)
        }
    }

    @Transactional
    fun getNextRecommendationQuotes(
        userId: Long,
        recommendationId: Long,
    ): List<QuoteResponse> {
        val recommendation = recommendationService.validateRecommendation(userId, recommendationId)
        recommendationService.validateRecommendationOngoing(recommendation)

        val recommendationHistory = recommendationService.getRecommendationHistory(recommendationId)
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

        recommendationService.createRecommendationQuotes(
            recommendationId = recommendationId,
            quoteIds = nextQuotes.map { quote -> quote.id },
        )

        return nextQuotes.map(::toQuoteResponse)
    }

    @Transactional
    fun selectRecommendationQuote(
        userId: Long,
        recommendationId: Long,
        quoteId: Long,
    ): RecommendationDetailResponse =
        toRecommendationDetailResponse(
            recommendationService.selectRecommendationQuote(
                userId = userId,
                recommendationId = recommendationId,
                quoteId = quoteId,
            ),
        )

    @Transactional
    fun deleteRecommendation(
        userId: Long,
        recommendationId: Long,
    ) {
        val recommendation = recommendationService.validateRecommendation(userId, recommendationId)
        recommendationService.validateRecommendationCompleted(recommendation)

        recommendationService.deleteRecommendation(recommendationId)
    }

    @Transactional(readOnly = true)
    fun getRecommendationDetail(
        userId: Long,
        recommendationId: Long,
    ): RecommendationDetailResponse {
        val recommendation =
            recommendationService.validateRecommendation(
                userId = userId,
                recommendationId = recommendationId,
                lockForUpdate = false,
            )

        return toRecommendationDetailResponse(recommendation)
    }

    @Transactional(readOnly = true)
    fun getRecommendationQuoteCandidates(
        userId: Long,
        recommendationId: Long,
    ): List<QuoteResponse> {
        recommendationService.validateRecommendation(
            userId = userId,
            recommendationId = recommendationId,
            lockForUpdate = false,
        )

        return recommendationService
            .getRecommendationHistory(recommendationId)
            .map { recommendationQuote -> quoteService.findQuoteById(recommendationQuote.quoteId) }
            .map(::toQuoteResponse)
    }

    @Transactional(readOnly = true)
    fun getRecommendationsByPeriod(
        userId: Long,
        start: LocalDate,
        end: LocalDate,
    ): RecommendationPeriodResponse {
        if (start.isAfter(end)) {
            throw CustomException(ErrorCode.INVALID_INPUT)
        }

        val recommendationItems =
            recommendationService
                .findCompletedByUserIdAndRecommendationDateBetween(
                    userId = userId,
                    start = start,
                    end = end,
                ).map { recommendation ->
                    val quoteId = checkNotNull(recommendation.quoteId)
                    val quote = quoteService.findQuoteById(quoteId)

                    RecommendationPeriodItemResponse.from(
                        recommendation = recommendation,
                        quote = toQuoteResponse(quote),
                    )
                }

        return RecommendationPeriodResponse.from(
            start = start,
            end = end,
            recommendations = recommendationItems,
        )
    }

    private fun firstRecommendedQuoteId(recommendationId: Long): Long =
        recommendationService
            .getRecommendationHistory(recommendationId)
            .firstOrNull()
            ?.quoteId
            ?: throw CustomException(ErrorCode.INVALID_RECOMMENDATION_QUOTE)

    private fun toRecommendationResponse(recommendation: Recommendation): RecommendationResponse {
        val quote = quoteService.findQuoteById(firstRecommendedQuoteId(recommendation.id))

        return toRecommendationResponse(
            recommendationId = recommendation.id,
            quote = quote,
        )
    }

    private fun toRecommendationResponse(
        recommendationId: Long,
        quote: Quote,
    ): RecommendationResponse =
        RecommendationResponse(
            recommendationId = recommendationId,
            quote = toQuoteResponse(quote),
        )

    private fun toRecommendationDetailResponse(recommendation: Recommendation): RecommendationDetailResponse {
        recommendationService.validateRecommendationCompleted(recommendation)

        val quoteId = recommendation.quoteId ?: throw CustomException(ErrorCode.RECOMMENDATION_NOT_COMPLETED)
        val quote = quoteService.findQuoteById(quoteId)
        val (emotionTags, needTag) = toTagDtos(recommendationService.getRecommendationTags(recommendation.id))

        return RecommendationDetailResponse(
            recommendationId = recommendation.id,
            quote = toQuoteResponse(quote),
            emotionRangeId = recommendation.emotionRangeId,
            emotionTags = emotionTags,
            needTag = needTag,
            feelingText = recommendation.feelingText,
            diaryText = recommendation.diaryText,
            recommendationDate = recommendation.recommendationDate,
        )
    }

    private fun toQuoteResponse(quote: Quote): QuoteResponse {
        val book = bookService.findBookById(quote.bookId)

        return QuoteResponse.from(
            quote = quote,
            book = book,
        )
    }

    private fun toTagDtos(recommendationTags: List<RecommendationTag>): Pair<List<TagDto>, TagDto?> {
        val tagIds = recommendationTags.map { recommendationTag -> recommendationTag.tagId }
        val (emotionTags, needTag) = emotionService.getEmotionTagsAndNeedTagByIds(tagIds)

        return emotionTags.map(TagDto::from) to needTag?.let(TagDto::from)
    }
}

private fun String?.hasText(): Boolean = !isNullOrBlank()

private fun String?.normalizedText(): String? = this?.trim()?.takeIf { text -> text.isNotEmpty() }
