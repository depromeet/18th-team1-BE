package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.recommendation.model.DailyRecommendation
import com.firstpenguin.app.domain.recommendation.model.DailyRecommendationQuote
import com.firstpenguin.app.domain.recommendation.model.DailyRecommendationTag
import com.firstpenguin.app.domain.recommendation.repository.DailyRecommendationQuoteRepository
import com.firstpenguin.app.domain.recommendation.repository.DailyRecommendationRepository
import com.firstpenguin.app.domain.recommendation.repository.DailyRecommendationTagRepository
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service
import java.time.LocalDate

private const val MAX_RECOMMENDATION_QUOTE_COUNT = 10

@Service
class RecommendationService(
    private val dailyRecommendationRepository: DailyRecommendationRepository,
    private val dailyRecommendationQuoteRepository: DailyRecommendationQuoteRepository,
    private val dailyRecommendationTagRepository: DailyRecommendationTagRepository,
) {
    fun createDailyRecommendation(
        userId: Long,
        quoteId: Long,
        userContext: String?,
        selectedEmotionRangeId: Long,
    ): Long =
        dailyRecommendationRepository.insertDailyRecommendation(
            userId = userId,
            quoteId = quoteId,
            userContext = userContext,
            selectedEmotionRangeId = selectedEmotionRangeId,
        )

    fun createDailyRecommendationQuotes(
        dailyRecommendationId: Long,
        quoteIds: List<Long>,
    ) {
        val maxDisplayOrder = dailyRecommendationQuoteRepository.getMaxDisplayOrder(dailyRecommendationId)

        dailyRecommendationQuoteRepository.insertDailyRecommendationQuote(
            dailyRecommendationId = dailyRecommendationId,
            quoteIds = quoteIds,
            nextDisplayOrder = maxDisplayOrder + 1,
        )
    }

    fun createDailyRecommendationTags(
        dailyRecommendationId: Long,
        tagIds: List<Long>,
    ) {
        dailyRecommendationTagRepository.insertDailyRecommendationTag(
            dailyRecommendationId = dailyRecommendationId,
            tagIds = tagIds,
        )
    }

    fun getRecommendationHistory(dailyRecommendationId: Long): List<DailyRecommendationQuote> =
        dailyRecommendationQuoteRepository
            .findByDailyRecommendationId(dailyRecommendationId)

    fun findByUserIdAndRecommendationDate(userId: Long): DailyRecommendation? {
        val today = LocalDate.now()

        return dailyRecommendationRepository.findByUserIdAndRecommendationDate(userId, today)
    }

    fun getRecommendationTags(dailyRecommendationId: Long): List<DailyRecommendationTag> =
        dailyRecommendationTagRepository.findByDailyRecommendationId(dailyRecommendationId)

    fun validateDailyRecommendationQuote(
        userId: Long,
        dailyRecommendationId: Long,
        quoteId: Long,
    ): DailyRecommendation {
        val dailyRecommendation = validateDailyRecommendation(userId, dailyRecommendationId)
        val isRecommendedQuote =
            dailyRecommendationQuoteRepository.existsByDailyRecommendationIdAndQuoteId(
                dailyRecommendationId = dailyRecommendationId,
                quoteId = quoteId,
            )

        if (!isRecommendedQuote) {
            throw CustomException(ErrorCode.INVALID_RECOMMENDATION_QUOTE)
        }

        return dailyRecommendation
    }

    fun validateDailyRecommendation(
        userId: Long,
        dailyRecommendationId: Long,
        lockForUpdate: Boolean = true,
    ): DailyRecommendation {
        val dailyRecommendation =
            if (lockForUpdate) {
                dailyRecommendationRepository.findDailyRecommendationByPkForUpdate(dailyRecommendationId)
            } else {
                dailyRecommendationRepository.findDailyRecommendationById(dailyRecommendationId)
            }
                ?: notFound()

        if (dailyRecommendation.userId != userId) {
            forbidden()
        }

        if (dailyRecommendation.recommendationDate != LocalDate.now()) {
            invalidDate()
        }

        return dailyRecommendation
    }

    fun validateRecommendationAvailable(userId: Long) {
        findByUserIdAndRecommendationDate(userId)?.let {
            throw CustomException(ErrorCode.DAILY_RECOMMENDATION_ALREADY_EXISTS)
        }
    }

    fun validateRecommendationCount(
        currentCount: Int,
        nextCount: Int,
    ) {
        if (currentCount + nextCount > MAX_RECOMMENDATION_QUOTE_COUNT) {
            throw CustomException(ErrorCode.EXCEEDED_DAILY_RECOMMENDATION_QUOTE_LIMIT)
        }
    }

    fun validateSelectedEmotionRange(
        dailyRecommendation: DailyRecommendation,
        emotionRangeId: Long,
    ) {
        if (dailyRecommendation.selectedEmotionRangeId != emotionRangeId) {
            throw CustomException(ErrorCode.INVALID_DIARY_EMOTION_VALUE)
        }
    }
}

private fun notFound(): Nothing = throw CustomException(ErrorCode.DAILY_RECOMMENDATION_NOT_FOUND)

private fun forbidden(): Nothing = throw CustomException(ErrorCode.FORBIDDEN_DAILY_RECOMMENDATION)

private fun invalidDate(): Nothing = throw CustomException(ErrorCode.INVALID_DAILY_RECOMMENDATION)
