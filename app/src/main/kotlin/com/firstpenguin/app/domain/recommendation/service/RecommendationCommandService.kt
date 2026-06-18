package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.recommendation.model.Recommendation
import com.firstpenguin.app.domain.recommendation.repository.RecommendationCommandRepository
import com.firstpenguin.app.domain.recommendation.repository.RecommendationQuoteRepository
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service

@Service
class RecommendationCommandService(
    private val recommendationCommandRepository: RecommendationCommandRepository,
    private val recommendationQuoteRepository: RecommendationQuoteRepository,
    private val recommendationValidationService: RecommendationValidationService,
) {
    fun selectRecommendationQuote(
        userId: Long,
        recommendationId: Long,
        quoteId: Long,
    ): Recommendation {
        val recommendation = recommendationValidationService.validateRecommendation(userId, recommendationId)
        recommendationValidationService.validateRecommendationOngoing(recommendation)

        if (!existsRecommendationQuote(recommendationId, quoteId)) {
            throw CustomException(ErrorCode.INVALID_RECOMMENDATION_QUOTE)
        }

        recommendationCommandRepository.updateQuoteId(
            id = recommendationId,
            quoteId = quoteId,
        )

        return recommendation.copy(quoteId = quoteId)
    }

    fun deleteRecommendation(recommendationId: Long) {
        recommendationCommandRepository.softDeleteById(recommendationId)
    }

    private fun existsRecommendationQuote(
        recommendationId: Long,
        quoteId: Long,
    ) = recommendationQuoteRepository.existsByRecommendationIdAndQuoteId(
        recommendationId = recommendationId,
        quoteId = quoteId,
    )
}
