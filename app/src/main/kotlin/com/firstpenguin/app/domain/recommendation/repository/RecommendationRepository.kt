package com.firstpenguin.app.domain.recommendation.repository

import com.firstpenguin.app.domain.emotion.repository.table.EmotionRangeTable
import com.firstpenguin.app.domain.recommendation.model.Recommendation
import com.firstpenguin.app.domain.recommendation.repository.table.RecommendationTable
import com.firstpenguin.app.global.enums.EmotionRangeName
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class RecommendationRepository(
    private val dsl: DSLContext,
) {
    fun findOngoingByUserIdAndRecommendationDate(
        userId: Long,
        recommendationDate: LocalDate,
    ): Recommendation? =
        dsl
            .select(RECOMMENDATION_FIELDS)
            .from(RecommendationTable.RECOMMENDATIONS)
            .join(EmotionRangeTable.EMOTION_RANGES)
            .on(RecommendationTable.EMOTION_RANGE_ID.eq(EmotionRangeTable.ID))
            .where(RecommendationTable.USER_ID.eq(userId))
            .and(RecommendationTable.RECOMMENDATION_DATE.eq(recommendationDate))
            .and(RecommendationTable.QUOTE_ID.isNull)
            .orderBy(RecommendationTable.CREATED_AT.desc())
            .limit(1)
            .fetchOne()
            ?.let(::toRecommendation)

    fun findRecommendationsByUserIdAndRecommendationDateBetween(
        userId: Long,
        start: LocalDate,
        end: LocalDate,
    ): List<Recommendation> =
        dsl
            .select(RECOMMENDATION_FIELDS)
            .from(RecommendationTable.RECOMMENDATIONS)
            .join(EmotionRangeTable.EMOTION_RANGES)
            .on(RecommendationTable.EMOTION_RANGE_ID.eq(EmotionRangeTable.ID))
            .where(RecommendationTable.USER_ID.eq(userId))
            .and(RecommendationTable.RECOMMENDATION_DATE.between(start, end))
            .orderBy(RecommendationTable.CREATED_AT.asc())
            .fetch(::toRecommendation)

    fun findCompletedRecommendationsByUserIdAndRecommendationDateBetween(
        userId: Long,
        start: LocalDate,
        end: LocalDate,
    ): List<Recommendation> =
        dsl
            .select(RECOMMENDATION_FIELDS)
            .from(RecommendationTable.RECOMMENDATIONS)
            .join(EmotionRangeTable.EMOTION_RANGES)
            .on(RecommendationTable.EMOTION_RANGE_ID.eq(EmotionRangeTable.ID))
            .where(RecommendationTable.USER_ID.eq(userId))
            .and(RecommendationTable.RECOMMENDATION_DATE.between(start, end))
            .and(RecommendationTable.QUOTE_ID.isNotNull)
            .orderBy(RecommendationTable.CREATED_AT.asc())
            .fetch(::toRecommendation)

    fun findRecommendationById(id: Long): Recommendation? =
        dsl
            .select(RECOMMENDATION_FIELDS)
            .from(RecommendationTable.RECOMMENDATIONS)
            .join(EmotionRangeTable.EMOTION_RANGES)
            .on(RecommendationTable.EMOTION_RANGE_ID.eq(EmotionRangeTable.ID))
            .where(RecommendationTable.ID.eq(id))
            .fetchOne()
            ?.let(::toRecommendation)

    fun findRecommendationByPkForUpdate(id: Long): Recommendation? =
        dsl
            .select(RECOMMENDATION_FIELDS)
            .from(RecommendationTable.RECOMMENDATIONS)
            .join(EmotionRangeTable.EMOTION_RANGES)
            .on(RecommendationTable.EMOTION_RANGE_ID.eq(EmotionRangeTable.ID))
            .where(RecommendationTable.ID.eq(id))
            .forUpdate()
            .fetchOne()
            ?.let(::toRecommendation)

    fun countByUserIdAndRecommendationDate(
        userId: Long,
        recommendationDate: LocalDate,
    ): Int =
        dsl
            .selectCount()
            .from(RecommendationTable.RECOMMENDATIONS)
            .where(RecommendationTable.USER_ID.eq(userId))
            .and(RecommendationTable.RECOMMENDATION_DATE.eq(recommendationDate))
            .fetchOne(0, Int::class.java) ?: 0

    fun countCompletedByUserId(userId: Long): Int =
        dsl
            .selectCount()
            .from(RecommendationTable.RECOMMENDATIONS)
            .where(RecommendationTable.USER_ID.eq(userId))
            .and(RecommendationTable.QUOTE_ID.isNotNull)
            .fetchOne(0, Int::class.java) ?: 0

    private fun toRecommendation(record: Record): Recommendation =
        Recommendation(
            id = record.get(RecommendationTable.ID),
            userId = record.get(RecommendationTable.USER_ID),
            quoteId = record.get(RecommendationTable.QUOTE_ID),
            recommendationDate = record.get(RecommendationTable.RECOMMENDATION_DATE),
            feelingText = record.get(RecommendationTable.FEELING_TEXT),
            diaryText = record.get(RecommendationTable.DIARY_TEXT),
            emotionRangeId = record.get(RecommendationTable.EMOTION_RANGE_ID),
            emotionRangeName = EmotionRangeName.from(record.get(EmotionRangeTable.NAME)),
            createdAt = record.get(RecommendationTable.CREATED_AT),
        )

    private companion object {
        val RECOMMENDATION_FIELDS: List<Field<*>> =
            listOf(
                RecommendationTable.ID,
                RecommendationTable.USER_ID,
                RecommendationTable.QUOTE_ID,
                RecommendationTable.RECOMMENDATION_DATE,
                RecommendationTable.FEELING_TEXT,
                RecommendationTable.DIARY_TEXT,
                RecommendationTable.EMOTION_RANGE_ID,
                EmotionRangeTable.NAME,
                RecommendationTable.CREATED_AT,
            )
    }
}
