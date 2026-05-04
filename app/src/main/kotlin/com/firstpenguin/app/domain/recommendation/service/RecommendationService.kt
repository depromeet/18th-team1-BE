package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.recommendation.repository.DailyRecommendationQuoteRepository
import com.firstpenguin.app.domain.recommendation.repository.DailyRecommendationRepository
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service
import java.time.LocalDate

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
    ) = dailyRecommendationRepository.insertDailyRecommendation(
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
            dailyRecommendationId,
            quoteIds,
            maxDisplayOrder + 1
        )
    }

    fun hasRecommendedToday(userId: Long): Boolean {
        val today = LocalDate.now()

        return dailyRecommendationRepository.existsByUserIdAndRecommendationDate(userId, today)
    }

    fun validateTodayRecommendation(userId: Long) {
        if (hasRecommendedToday(userId)) {
            throw CustomException(ErrorCode.DAILY_RECOMMENDATION_ALREADY_EXISTS)
        }
    }
}
