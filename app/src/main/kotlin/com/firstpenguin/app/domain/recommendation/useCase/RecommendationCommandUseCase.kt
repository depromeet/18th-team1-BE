package com.firstpenguin.app.domain.recommendation.useCase

import com.firstpenguin.app.domain.quote.service.QuoteService
import com.firstpenguin.app.domain.recommendation.dto.RecommendationRequest
import com.firstpenguin.app.domain.recommendation.dto.RecommendationResponse
import com.firstpenguin.app.domain.recommendation.model.RecommendationResult
import com.firstpenguin.app.domain.recommendation.service.RecommendationAnalysisLogService
import com.firstpenguin.app.domain.recommendation.service.RecommendationResponseMapper
import com.firstpenguin.app.domain.recommendation.service.RecommendationService
import com.firstpenguin.app.domain.recommendation.service.RecommendationValidationService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RecommendationCommandUseCase(
    private val quoteService: QuoteService,
    private val recommendationService: RecommendationService,
    private val recommendationValidationService: RecommendationValidationService,
    private val recommendationResponseMapper: RecommendationResponseMapper,
    private val recommendationAnalysisLogService: RecommendationAnalysisLogService,
) {
    @Transactional
    fun saveRecommendationResult(
        userId: Long,
        request: RecommendationRequest,
        result: RecommendationResult,
    ): RecommendationResponse {
        recommendationService.lockRecommendationCreation(userId)

        val ongoingRecommendation = recommendationService.findOngoingRecommendation(userId)
        if (ongoingRecommendation != null) {
            return recommendationResponseMapper.toRecommendationResponse(ongoingRecommendation)
        }

        return saveNewRecommendationResult(userId, request, result)
    }

    private fun saveNewRecommendationResult(
        userId: Long,
        request: RecommendationRequest,
        result: RecommendationResult,
    ): RecommendationResponse {
        recommendationValidationService.validateRecommendationCreatable(userId)
        val recommendationId = createRecommendation(userId, request)

        recommendationService.createRecommendationTags(
            recommendationId = recommendationId,
            tagIds = request.selectedTagIds(),
        )
        recommendationService.createRankedRecommendationQuotes(
            recommendationId = recommendationId,
            rankedQuotes = result.quotes,
        )
        result.analysisLog?.let { analysisLog ->
            recommendationAnalysisLogService.saveRecommendationAnalysisLog(
                recommendationId = recommendationId,
                analysisLog = analysisLog,
            )
        }

        return recommendationResponseMapper.toRecommendationResponse(
            recommendationId = recommendationId,
            quote = quoteService.findQuoteById(result.mainQuote.quoteId),
        )
    }

    private fun createRecommendation(
        userId: Long,
        request: RecommendationRequest,
    ): Long =
        recommendationService.createRecommendation(
            userId = userId,
            feelingText = request.feelingText.normalizedText(),
            diaryText = request.diaryText,
            emotionRangeId = request.emotionRangeId,
        )

    private fun RecommendationRequest.selectedTagIds(): List<Long> = emotionTagIds + listOfNotNull(needTagId)
}

private fun String?.normalizedText(): String? = this?.trim()?.takeIf { text -> text.isNotEmpty() }
