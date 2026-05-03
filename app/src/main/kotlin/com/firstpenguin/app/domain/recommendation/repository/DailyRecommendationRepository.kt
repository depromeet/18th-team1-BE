package com.firstpenguin.app.domain.recommendation.repository

import com.firstpenguin.app.domain.recommendation.model.DailyRecommendation
import com.firstpenguin.app.domain.recommendation.repository.table.DailyRecommendationTable
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
class DailyRecommendationRepository(
    private val dsl: DSLContext,
) {
    fun insertDailyRecommendation(
        userId: Long,
        quoteId: Long,
        userContext: String,
        selectedEmotionRangeId: Long,
    ) {
        val now = LocalDateTime.now()
        val today = now.toLocalDate()

        dsl
            .insertInto(DailyRecommendationTable.DAILY_RECOMMENDATIONS)
            .set(DailyRecommendationTable.USER_ID, userId)
            .set(DailyRecommendationTable.QUOTE_ID, quoteId)
            .set(DailyRecommendationTable.RECOMMENDATION_DATE, today)
            .set(DailyRecommendationTable.USER_CONTEXT, userContext)
            .set(DailyRecommendationTable.SELECTED_EMOTION_RANGE_ID, selectedEmotionRangeId)
            .set(DailyRecommendationTable.CREATED_AT, now)
            .execute()
    }

    fun existsByUserIdAndRecommendationDate(
        userId: Long,
        recommendationDate: LocalDate,
    ): Boolean =
        dsl.fetchExists(
            DailyRecommendationTable.DAILY_RECOMMENDATIONS,
            DailyRecommendationTable.USER_ID
                .eq(userId)
                .and(DailyRecommendationTable.RECOMMENDATION_DATE.eq(recommendationDate)),
        )

    fun findDailyRecommendationByPk(id: Long): DailyRecommendation? =
        dsl
            .select(DAILY_RECOMMENDATION_FIELDS)
            .from(DailyRecommendationTable.DAILY_RECOMMENDATIONS)
            .where(DailyRecommendationTable.ID.eq(id))
            .fetchOne(::toDailyRecommendation)

    private fun toDailyRecommendation(record: Record): DailyRecommendation =
        DailyRecommendation(
            id = record.get(DailyRecommendationTable.ID),
            userId = record.get(DailyRecommendationTable.USER_ID),
            quoteId = record.get(DailyRecommendationTable.QUOTE_ID),
            recommendationDate = record.get(DailyRecommendationTable.RECOMMENDATION_DATE),
            userContext = record.get(DailyRecommendationTable.USER_CONTEXT),
            selectedEmotionRangeId = record.get(DailyRecommendationTable.SELECTED_EMOTION_RANGE_ID),
            createdAt = record.get(DailyRecommendationTable.CREATED_AT),
        )

    private companion object {
        val DAILY_RECOMMENDATION_FIELDS: List<Field<*>> =
            listOf(
                DailyRecommendationTable.ID,
                DailyRecommendationTable.USER_ID,
                DailyRecommendationTable.QUOTE_ID,
                DailyRecommendationTable.RECOMMENDATION_DATE,
                DailyRecommendationTable.USER_CONTEXT,
                DailyRecommendationTable.SELECTED_EMOTION_RANGE_ID,
                DailyRecommendationTable.CREATED_AT,
            )
    }
}
