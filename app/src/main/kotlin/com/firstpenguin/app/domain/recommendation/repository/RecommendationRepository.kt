package com.firstpenguin.app.domain.recommendation.repository

import com.firstpenguin.app.domain.recommendation.model.Recommendation
import com.firstpenguin.app.domain.recommendation.repository.table.RecommendationTable
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
class RecommendationRepository(
    private val dsl: DSLContext,
) {
    fun insertRecommendation(
        userId: Long,
        feelingText: String?,
        diaryText: String?,
        emotionRangeId: Long,
    ): Long {
        val now = LocalDateTime.now()
        val today = now.toLocalDate()

        return dsl
            .insertInto(RecommendationTable.RECOMMENDATIONS)
            .set(RecommendationTable.USER_ID, userId)
            .set(RecommendationTable.RECOMMENDATION_DATE, today)
            .set(RecommendationTable.FEELING_TEXT, feelingText)
            .set(RecommendationTable.DIARY_TEXT, diaryText)
            .set(RecommendationTable.SELECTED_EMOTION_RANGE_ID, emotionRangeId)
            .set(RecommendationTable.CREATED_AT, now)
            .returning(RecommendationTable.ID)
            .fetchOne(RecommendationTable.ID)
            ?: throw CustomException(ErrorCode.RECOMMENDATION_CREATE_FAILED)
    }

    fun findByUserIdAndRecommendationDate(
        userId: Long,
        recommendationDate: LocalDate,
    ): Recommendation? =
        dsl
            .select(RECOMMENDATION_FIELDS)
            .from(RecommendationTable.RECOMMENDATIONS)
            .where(RecommendationTable.USER_ID.eq(userId))
            .and(RecommendationTable.RECOMMENDATION_DATE.eq(recommendationDate))
            .orderBy(RecommendationTable.CREATED_AT.desc())
            .limit(1)
            .fetchOne()
            ?.let(::toRecommendation)

    fun findOngoingByUserIdAndRecommendationDate(
        userId: Long,
        recommendationDate: LocalDate,
    ): Recommendation? =
        dsl
            .select(RECOMMENDATION_FIELDS)
            .from(RecommendationTable.RECOMMENDATIONS)
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
            .where(RecommendationTable.USER_ID.eq(userId))
            .and(RecommendationTable.RECOMMENDATION_DATE.between(start, end))
            .and(RecommendationTable.QUOTE_ID.isNotNull)
            .orderBy(RecommendationTable.CREATED_AT.asc())
            .fetch(::toRecommendation)

    fun findRecommendationById(id: Long): Recommendation? =
        dsl
            .select(RECOMMENDATION_FIELDS)
            .from(RecommendationTable.RECOMMENDATIONS)
            .where(RecommendationTable.ID.eq(id))
            .fetchOne()
            ?.let(::toRecommendation)

    fun findRecommendationByPkForUpdate(id: Long): Recommendation? =
        dsl
            .select(RECOMMENDATION_FIELDS)
            .from(RecommendationTable.RECOMMENDATIONS)
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

    private fun toRecommendation(record: Record): Recommendation =
        Recommendation(
            id = record.get(RecommendationTable.ID),
            userId = record.get(RecommendationTable.USER_ID),
            quoteId = record.get(RecommendationTable.QUOTE_ID),
            recommendationDate = record.get(RecommendationTable.RECOMMENDATION_DATE),
            feelingText = record.get(RecommendationTable.FEELING_TEXT),
            diaryText = record.get(RecommendationTable.DIARY_TEXT),
            emotionRangeId = record.get(RecommendationTable.SELECTED_EMOTION_RANGE_ID),
            createdAt = record.get(RecommendationTable.CREATED_AT),
        )

    private companion object {
        const val RECOMMENDATION_CREATION_LOCK_USER_FACTOR = 100_000L

        val RECOMMENDATION_FIELDS: List<Field<*>> =
            listOf(
                RecommendationTable.ID,
                RecommendationTable.USER_ID,
                RecommendationTable.QUOTE_ID,
                RecommendationTable.RECOMMENDATION_DATE,
                RecommendationTable.FEELING_TEXT,
                RecommendationTable.DIARY_TEXT,
                RecommendationTable.SELECTED_EMOTION_RANGE_ID,
                RecommendationTable.CREATED_AT,
            )

        fun recommendationCreationLockKey(
            userId: Long,
            recommendationDate: LocalDate,
        ): Long = userId * RECOMMENDATION_CREATION_LOCK_USER_FACTOR + recommendationDate.toEpochDay()
    }
}
