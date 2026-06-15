package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.recommendation.model.RankedRecommendationQuote
import com.firstpenguin.app.domain.recommendation.model.Recommendation
import com.firstpenguin.app.domain.recommendation.model.RecommendationQuote
import com.firstpenguin.app.domain.recommendation.model.RecommendationTag
import com.firstpenguin.app.domain.recommendation.repository.RecommendationCommandRepository
import com.firstpenguin.app.domain.recommendation.repository.RecommendationQuoteRepository
import com.firstpenguin.app.domain.recommendation.repository.RecommendationRepository
import com.firstpenguin.app.domain.recommendation.repository.RecommendationTagRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class RecommendationService(
    private val recommendationRepository: RecommendationRepository,
    private val recommendationCommandRepository: RecommendationCommandRepository,
    private val recommendationQuoteRepository: RecommendationQuoteRepository,
    private val recommendationTagRepository: RecommendationTagRepository,
) {
    fun createRecommendation(
        userId: Long,
        feelingText: String?,
        diaryText: String?,
        emotionRangeId: Long,
    ): Long =
        recommendationCommandRepository.insertRecommendation(
            userId = userId,
            feelingText = feelingText,
            diaryText = diaryText,
            emotionRangeId = emotionRangeId,
        )

    fun createRankedRecommendationQuotes(
        recommendationId: Long,
        rankedQuotes: List<RankedRecommendationQuote>,
    ) {
        val maxDisplayOrder = recommendationQuoteRepository.getMaxDisplayOrder(recommendationId)

        recommendationQuoteRepository.insertRankedRecommendationQuotes(
            recommendationId = recommendationId,
            rankedQuotes = rankedQuotes,
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

        recommendationCommandRepository.lockRecommendationCreation(
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

    fun hasReachedRecommendationLimit(count: Int): Boolean = count >= MAX_RECOMMENDATION_COUNT_PER_DAY

    private companion object {
        const val MAX_RECOMMENDATION_COUNT_PER_DAY = 5
    }
}
