package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.recommendation.model.Recommendation
import com.firstpenguin.app.domain.recommendation.repository.RecommendationRepository
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service
import java.time.LocalDate

private const val MAX_RECOMMENDATION_QUOTE_COUNT = 10
private const val MAX_RECOMMENDATION_COUNT_PER_DAY = 5

@Service
class RecommendationValidationService(
    private val recommendationRepository: RecommendationRepository,
) {
    fun validateRecommendationCreatable(userId: Long) {
        val count =
            recommendationRepository.countByUserIdAndRecommendationDate(
                userId = userId,
                recommendationDate = LocalDate.now(),
            )

        if (count >= MAX_RECOMMENDATION_COUNT_PER_DAY) {
            throw CustomException(ErrorCode.RECOMMENDATION_LIMIT_EXCEEDED)
        }
    }

    fun validateRecommendationCount(
        currentCount: Int,
        nextCount: Int,
    ) {
        if (currentCount + nextCount > MAX_RECOMMENDATION_QUOTE_COUNT) {
            throw CustomException(ErrorCode.EXCEEDED_RECOMMENDATION_QUOTE_LIMIT)
        }
    }

    fun validateRecommendationOngoing(recommendation: Recommendation) {
        if (recommendation.quoteId != null) {
            throw CustomException(ErrorCode.RECOMMENDATION_ALREADY_COMPLETED)
        }
    }

    fun validateRecommendationCompleted(recommendation: Recommendation) {
        if (recommendation.quoteId == null) {
            throw CustomException(ErrorCode.RECOMMENDATION_NOT_COMPLETED)
        }
    }

    fun validateRecommendation(
        userId: Long,
        recommendationId: Long,
        lockForUpdate: Boolean = true,
    ): Recommendation {
        val recommendation =
            if (lockForUpdate) {
                recommendationRepository.findRecommendationByPkForUpdate(recommendationId)
            } else {
                recommendationRepository.findRecommendationById(recommendationId)
            }
                ?: notFound()

        if (recommendation.userId != userId) {
            forbidden()
        }

        return recommendation
    }
}

private fun notFound(): Nothing = throw CustomException(ErrorCode.RECOMMENDATION_NOT_FOUND)

private fun forbidden(): Nothing = throw CustomException(ErrorCode.FORBIDDEN_RECOMMENDATION)
