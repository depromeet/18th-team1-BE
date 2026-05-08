package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.recommendation.model.DailyRecommendation
import com.firstpenguin.app.domain.recommendation.model.DailyRecommendationQuote
import com.firstpenguin.app.domain.recommendation.repository.DailyRecommendationQuoteRepository
import com.firstpenguin.app.domain.recommendation.repository.DailyRecommendationRepository
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service
import java.time.LocalDate

private const val MAX_RECOMMENDATION_QUOTE_COUNT = 10

@Service
class RecommendationService(
    private val dailyRecommendationRepository: DailyRecommendationRepository,
    private val dailyRecommendationQuoteRepository: DailyRecommendationQuoteRepository,
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

    fun getDailyRecommendation(id: Long) =
        dailyRecommendationRepository.findDailyRecommendationByPkForUpdate(id)
            ?: throw CustomException(ErrorCode.DAILY_RECOMMENDATION_NOT_FOUND)

    fun getRecommendationHistory(dailyRecommendationId: Long): List<DailyRecommendationQuote> =
        dailyRecommendationQuoteRepository
            .findByDailyRecommendationId(dailyRecommendationId)

    fun hasRecommendedToday(userId: Long): Boolean {
        val today = LocalDate.now()

        return dailyRecommendationRepository.existsByUserIdAndRecommendationDate(userId, today)
    }

    fun validateRecommendationAvailable(userId: Long) {
        if (hasRecommendedToday(userId)) {
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

    fun validateTodayRecommendation(dailyRecommendationDate: LocalDate) {
        if (dailyRecommendationDate != LocalDate.now()) {
            throw CustomException(ErrorCode.INVALID_DAILY_RECOMMENDATION)
        }
    }

    fun validateRecommendedQuote(
        dailyRecommendationId: Long,
        quoteId: Long,
    ) {
        val isRecommendedQuote =
            dailyRecommendationQuoteRepository.existsByDailyRecommendationIdAndQuoteId(
                dailyRecommendationId = dailyRecommendationId,
                quoteId = quoteId,
            )

        if (!isRecommendedQuote) {
            throw CustomException(ErrorCode.INVALID_RECOMMENDATION_QUOTE)
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

    fun validateOwner(
        userId: Long,
        dailyRecommendation: DailyRecommendation,
    ) {
        if (dailyRecommendation.userId != userId) {
            throw CustomException(ErrorCode.FORBIDDEN_DAILY_RECOMMENDATION)
        }
    }
}
