package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.recommendation.model.Recommendation
import com.firstpenguin.app.domain.recommendation.model.RecommendationQuote
import com.firstpenguin.app.domain.recommendation.model.RecommendationTag
import com.firstpenguin.app.domain.recommendation.repository.RecommendationQuoteRepository
import com.firstpenguin.app.domain.recommendation.repository.RecommendationRepository
import com.firstpenguin.app.domain.recommendation.repository.RecommendationTagRepository
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service
import java.time.LocalDate

private const val MAX_RECOMMENDATION_QUOTE_COUNT = 10

@Service
class RecommendationService(
    private val recommendationRepository: RecommendationRepository,
    private val recommendationQuoteRepository: RecommendationQuoteRepository,
    private val recommendationTagRepository: RecommendationTagRepository,
) {
    fun createRecommendation(
        userId: Long,
        feelingText: String?,
        diaryText: String?,
        emotionRangeId: Long,
    ): Long =
        recommendationRepository.insertRecommendation(
            userId = userId,
            feelingText = feelingText,
            diaryText = diaryText,
            emotionRangeId = emotionRangeId,
        )

    fun createRecommendationQuotes(
        recommendationId: Long,
        quoteIds: List<Long>,
    ) {
        val maxDisplayOrder = recommendationQuoteRepository.getMaxDisplayOrder(recommendationId)

        recommendationQuoteRepository.insertRecommendationQuote(
            recommendationId = recommendationId,
            quoteIds = quoteIds,
            nextDisplayOrder = maxDisplayOrder + 1,
        )
    }

    fun createRecommendationTags(
        recommendationId: Long,
        tagIds: List<Long>,
    ) {
        recommendationTagRepository.insertRecommendationTag(
            recommendationId = recommendationId,
            tagIds = tagIds,
        )
    }

    fun getRecommendationHistory(recommendationId: Long): List<RecommendationQuote> =
        recommendationQuoteRepository
            .findByRecommendationId(recommendationId)

    fun lockRecommendationCreation(userId: Long) {
        val today = LocalDate.now()

        recommendationRepository.lockRecommendationCreation(
            userId = userId,
            recommendationDate = today,
        )
    }

    fun findOngoingRecommendation(userId: Long): Recommendation? {
        val today = LocalDate.now()

        return recommendationRepository.findOngoingByUserIdAndRecommendationDate(
            userId = userId,
            recommendationDate = today,
        )
    }

    fun findByUserIdAndRecommendationDate(userId: Long): Recommendation? {
        val today = LocalDate.now()

        return recommendationRepository.findByUserIdAndRecommendationDate(userId, today)
    }

    fun findByUserIdAndRecommendationDateBetween(
        userId: Long,
        start: LocalDate,
        end: LocalDate,
    ): List<Recommendation> =
        recommendationRepository.findRecommendationsByUserIdAndRecommendationDateBetween(
            userId = userId,
            start = start,
            end = end,
        )

    fun findCompletedByUserIdAndRecommendationDateBetween(
        userId: Long,
        start: LocalDate,
        end: LocalDate,
    ): List<Recommendation> =
        recommendationRepository.findCompletedRecommendationsByUserIdAndRecommendationDateBetween(
            userId = userId,
            start = start,
            end = end,
        )

    fun countCompletedByUserId(userId: Long): Int = recommendationRepository.countCompletedByUserId(userId)

    fun getRecommendationTags(recommendationId: Long): List<RecommendationTag> =
        recommendationTagRepository.findByRecommendationId(recommendationId)

    fun deleteRecommendation(recommendationId: Long) {
        recommendationQuoteRepository.deleteByRecommendationId(recommendationId)
        recommendationTagRepository.deleteByRecommendationId(recommendationId)
        recommendationRepository.deleteById(recommendationId)
    }

    fun selectRecommendationQuote(
        userId: Long,
        recommendationId: Long,
        quoteId: Long,
    ): Recommendation {
        val recommendation = validateRecommendation(userId, recommendationId)
        validateRecommendationOngoing(recommendation)

        if (!isExistRecommendationQuote(recommendationId, quoteId)) {
            throw CustomException(ErrorCode.INVALID_RECOMMENDATION_QUOTE)
        }

        recommendationRepository.updateQuoteId(
            id = recommendationId,
            quoteId = quoteId,
        )

        return recommendation.copy(quoteId = quoteId)
    }

    fun validateRecommendationCreatable(userId: Long) {
        val today = LocalDate.now()
        val count =
            recommendationRepository.countByUserIdAndRecommendationDate(
                userId = userId,
                recommendationDate = today,
            )

        if (hasReachedRecommendationLimit(count)) {
            throw CustomException(ErrorCode.RECOMMENDATION_LIMIT_EXCEEDED)
        }
    }

    fun hasReachedRecommendationLimit(recommendationCount: Int): Boolean = recommendationCount >= MAX_RECOMMENDATION_COUNT_PER_DAY

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
                findRecommendationByIdForUpdate(recommendationId)
            } else {
                findRecommendationById(recommendationId)
            }
                ?: notFound()

        if (recommendation.userId != userId) {
            forbidden()
        }

        return recommendation
    }

    fun findRecommendationById(recommendationId: Long) = recommendationRepository.findRecommendationById(recommendationId)

    fun findRecommendationByIdForUpdate(recommendationId: Long) = recommendationRepository.findRecommendationByPkForUpdate(recommendationId)

    private fun isExistRecommendationQuote(
        recommendationId: Long,
        quoteId: Long,
    ) = recommendationQuoteRepository.existsByRecommendationIdAndQuoteId(
        recommendationId = recommendationId,
        quoteId = quoteId,
    )

    private companion object {
        const val MAX_RECOMMENDATION_COUNT_PER_DAY = 5
    }
}

private fun notFound(): Nothing = throw CustomException(ErrorCode.RECOMMENDATION_NOT_FOUND)

private fun forbidden(): Nothing = throw CustomException(ErrorCode.FORBIDDEN_RECOMMENDATION)
