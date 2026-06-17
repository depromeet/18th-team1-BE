package com.firstpenguin.app.domain.recommendation.repository

import com.firstpenguin.app.domain.emotion.repository.table.EmotionRangeTable
import com.firstpenguin.app.domain.recommendation.repository.table.RecommendationTable
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
class RecommendationCommandRepository(
    private val dsl: DSLContext,
) {
    fun insertRecommendation(
        userId: Long,
        feelingText: String?,
        diaryText: String?,
        emotionValue: Int,
    ): Long {
        val now = LocalDateTime.now()

        return dsl
            .insertInto(RecommendationTable.RECOMMENDATIONS)
            .set(RecommendationTable.USER_ID, userId)
            .set(RecommendationTable.RECOMMENDATION_DATE, now.toLocalDate())
            .set(RecommendationTable.FEELING_TEXT, feelingText)
            .set(RecommendationTable.DIARY_TEXT, diaryText)
            .set(RecommendationTable.EMOTION_VALUE, emotionValue)
            .set(RecommendationTable.EMOTION_RANGE_ID, emotionRangeIdByValue(emotionValue))
            .set(RecommendationTable.CREATED_AT, now)
            .returning(RecommendationTable.ID)
            .fetchOne(RecommendationTable.ID)
            ?: throw CustomException(ErrorCode.RECOMMENDATION_CREATE_FAILED)
    }

    fun lockRecommendationCreation(
        userId: Long,
        recommendationDate: LocalDate,
    ) {
        val lockKey = recommendationCreationLockKey(userId, recommendationDate)

        dsl.execute("SELECT pg_advisory_xact_lock(?)", lockKey)
    }

    fun updateQuoteId(
        id: Long,
        quoteId: Long,
    ) {
        dsl
            .update(RecommendationTable.RECOMMENDATIONS)
            .set(RecommendationTable.QUOTE_ID, quoteId)
            .where(RecommendationTable.ID.eq(id))
            .execute()
    }

    fun deleteById(id: Long) {
        dsl
            .deleteFrom(RecommendationTable.RECOMMENDATIONS)
            .where(RecommendationTable.ID.eq(id))
            .execute()
    }

    private companion object {
        const val RECOMMENDATION_CREATION_LOCK_USER_FACTOR = 100_000L

        fun emotionRangeIdByValue(emotionValue: Int) =
            DSL
                .select(EmotionRangeTable.ID)
                .from(EmotionRangeTable.EMOTION_RANGES)
                .where(EmotionRangeTable.MIN_VALUE.le(emotionValue))
                .and(EmotionRangeTable.MAX_VALUE.ge(emotionValue))
                .limit(1)

        fun recommendationCreationLockKey(
            userId: Long,
            recommendationDate: LocalDate,
        ): Long = userId * RECOMMENDATION_CREATION_LOCK_USER_FACTOR + recommendationDate.toEpochDay()
    }
}
