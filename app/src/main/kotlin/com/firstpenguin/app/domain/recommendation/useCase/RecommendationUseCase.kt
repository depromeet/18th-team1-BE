package com.firstpenguin.app.domain.recommendation.useCase

import com.firstpenguin.app.domain.quote.dto.QuoteResponse
import com.firstpenguin.app.domain.quote.service.QuoteService
import com.firstpenguin.app.domain.recommendation.dto.RecommendationDetailResponse
import com.firstpenguin.app.domain.recommendation.dto.RecommendationPeriodItemResponse
import com.firstpenguin.app.domain.recommendation.dto.RecommendationPeriodResponse
import com.firstpenguin.app.domain.recommendation.dto.RecommendationRequest
import com.firstpenguin.app.domain.recommendation.dto.RecommendationResponse
import com.firstpenguin.app.domain.recommendation.service.RecommendationCommandService
import com.firstpenguin.app.domain.recommendation.service.RecommendationEngine
import com.firstpenguin.app.domain.recommendation.service.RecommendationRequestValidator
import com.firstpenguin.app.domain.recommendation.service.RecommendationResponseMapper
import com.firstpenguin.app.domain.recommendation.service.RecommendationService
import com.firstpenguin.app.domain.recommendation.service.RecommendationValidationService
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

private const val NEXT_RECOMMENDATION_QUOTE_COUNT = 9
private const val FIRST_RECOMMENDATION_QUOTE_COUNT = 1

@Service
class RecommendationUseCase(
    private val quoteService: QuoteService,
    private val recommendationEngine: RecommendationEngine,
    private val recommendationCommandUseCase: RecommendationCommandUseCase,
    private val recommendationService: RecommendationService,
    private val recommendationCommandService: RecommendationCommandService,
    private val recommendationValidationService: RecommendationValidationService,
    private val recommendationRequestValidator: RecommendationRequestValidator,
    private val recommendationResponseMapper: RecommendationResponseMapper,
) {
    fun recommendQuote(
        userId: Long,
        request: RecommendationRequest,
    ): RecommendationResponse {
        recommendationRequestValidator.validate(request)
        recommendationService.findOngoingRecommendation(userId)?.let { recommendation ->
            return recommendationResponseMapper.toRecommendationResponse(recommendation)
        }
        recommendationValidationService.validateRecommendationCreatable(userId)

        return recommendationCommandUseCase.saveRecommendationResult(
            userId = userId,
            request = request,
            result = recommendationEngine.recommend(userId, request),
        )
    }

    @Transactional(readOnly = true)
    fun getNextRecommendationQuotes(
        userId: Long,
        recommendationId: Long,
    ): List<QuoteResponse> {
        val recommendation =
            recommendationValidationService.validateRecommendation(
                userId = userId,
                recommendationId = recommendationId,
                lockForUpdate = false,
            )
        recommendationValidationService.validateRecommendationOngoing(recommendation)

        return recommendationService
            .getRecommendationHistory(recommendationId)
            .drop(FIRST_RECOMMENDATION_QUOTE_COUNT)
            .take(NEXT_RECOMMENDATION_QUOTE_COUNT)
            .map { recommendationQuote -> quoteService.findQuoteById(recommendationQuote.quoteId) }
            .map(recommendationResponseMapper::toQuoteResponse)
    }

    @Transactional
    fun selectRecommendationQuote(
        userId: Long,
        recommendationId: Long,
        quoteId: Long,
    ): RecommendationDetailResponse =
        recommendationResponseMapper.toRecommendationDetailResponse(
            recommendationCommandService.selectRecommendationQuote(
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
        val recommendation = recommendationValidationService.validateRecommendation(userId, recommendationId)
        recommendationValidationService.validateRecommendationCompleted(recommendation)

        recommendationCommandService.deleteRecommendation(recommendationId)
    }

    @Transactional(readOnly = true)
    fun getRecommendationDetail(
        userId: Long,
        recommendationId: Long,
    ): RecommendationDetailResponse {
        val recommendation =
            recommendationValidationService.validateRecommendation(
                userId = userId,
                recommendationId = recommendationId,
                lockForUpdate = false,
            )
        recommendationValidationService.validateRecommendationCompleted(recommendation)

        return recommendationResponseMapper.toRecommendationDetailResponse(recommendation)
    }

    @Transactional(readOnly = true)
    fun getRecommendationQuoteCandidates(
        userId: Long,
        recommendationId: Long,
    ): List<QuoteResponse> {
        recommendationValidationService.validateRecommendation(
            userId = userId,
            recommendationId = recommendationId,
            lockForUpdate = false,
        )

        return recommendationService
            .getRecommendationHistory(recommendationId)
            .map { recommendationQuote -> quoteService.findQuoteById(recommendationQuote.quoteId) }
            .map(recommendationResponseMapper::toQuoteResponse)
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
                        quote = recommendationResponseMapper.toQuoteResponse(quote),
                    )
                }

        return RecommendationPeriodResponse.from(
            start = start,
            end = end,
            recommendations = recommendationItems,
        )
    }
}
