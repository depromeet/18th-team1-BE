package com.firstpenguin.app.domain.recommendation.useCase

import com.firstpenguin.app.domain.quote.service.QuoteService
import com.firstpenguin.app.domain.recommendation.dto.RecommendationRequest
import com.firstpenguin.app.domain.recommendation.dto.RecommendationResponse
import com.firstpenguin.app.domain.recommendation.model.RecommendationResult
import com.firstpenguin.app.domain.recommendation.service.RecommendationResponseMapper
import com.firstpenguin.app.domain.recommendation.service.RecommendationService
import com.firstpenguin.app.domain.recommendation.service.RecommendationValidationService
import com.firstpenguin.app.domain.recommendation.service.measureRecommendationStep
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RecommendationCommandUseCase(
    private val quoteService: QuoteService,
    private val recommendationService: RecommendationService,
    private val recommendationValidationService: RecommendationValidationService,
    private val recommendationResponseMapper: RecommendationResponseMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun saveRecommendationResult(
        userId: Long,
        request: RecommendationRequest,
        result: RecommendationResult,
    ): RecommendationResponse {
        lateinit var response: RecommendationResponse

        log.measureRecommendationStep("command.saveRecommendationResult.total", { "userId=$userId" }) {
            log.measureRecommendationStep("command.lockRecommendationCreation", { "userId=$userId" }) {
                recommendationService.lockRecommendationCreation(userId)
            }

            val ongoingRecommendation =
                log.measureRecommendationStep("command.ongoingLookup", { "userId=$userId" }) {
                    recommendationService.findOngoingRecommendation(userId)
                }
            if (ongoingRecommendation != null) {
                response = recommendationResponseMapper.toRecommendationResponse(ongoingRecommendation)
                return@measureRecommendationStep
            }

            response = saveNewRecommendationResult(userId, request, result)
        }

        return response
    }

    private fun saveNewRecommendationResult(
        userId: Long,
        request: RecommendationRequest,
        result: RecommendationResult,
    ): RecommendationResponse {
        log.measureRecommendationStep("command.validateCreatable", { "userId=$userId" }) {
            recommendationValidationService.validateRecommendationCreatable(userId)
        }
        val recommendationId =
            log.measureRecommendationStep("command.createRecommendation", { "userId=$userId" }) {
                createRecommendation(userId, request)
            }

        log.measureRecommendationStep("command.createRecommendationTags", { "recommendationId=$recommendationId" }) {
            recommendationService.createRecommendationTags(
                recommendationId = recommendationId,
                tagIds = request.selectedTagIds(),
            )
        }
        log.measureRecommendationStep(
            "command.createRecommendationQuotes",
            { "recommendationId=$recommendationId quoteCount=${result.quotes.size}" },
        ) {
            recommendationService.createRankedRecommendationQuotes(
                recommendationId = recommendationId,
                rankedQuotes = result.quotes,
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
