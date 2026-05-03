package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.recommendation.repository.DailyRecommendationRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class RecommendationService(
    private val dailyRecommendationRepository: DailyRecommendationRepository,
) {
    fun createDailyRecommendation(
        userId: Long,
        quoteId: Long,
        userContext: String,
        selectedEmotionRangeId: Long
    ) {
        dailyRecommendationRepository.insertDailyRecommendation(userId, quoteId, userContext, selectedEmotionRangeId)
    }

    fun hasRecommendedToday(userId: Long): Boolean {
        val today = LocalDate.now()

        return dailyRecommendationRepository.existsByUserIdAndRecommendationDate(userId, today)
    }
}
