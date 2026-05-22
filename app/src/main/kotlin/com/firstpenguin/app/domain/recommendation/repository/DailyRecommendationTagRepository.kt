package com.firstpenguin.app.domain.recommendation.repository

import com.firstpenguin.app.domain.recommendation.model.DailyRecommendationTag
import com.firstpenguin.app.domain.recommendation.repository.table.DailyRecommendationTagTable
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class DailyRecommendationTagRepository(
    private val dsl: DSLContext,
) {
    fun insertDailyRecommendationTag(
        dailyRecommendationId: Long,
        tagIds: List<Long>,
    ) {
        if (tagIds.isEmpty()) return

        val rows =
            tagIds
                .distinct()
                .map { tagId -> DSL.row(dailyRecommendationId, tagId) }
        val insertStep =
            dsl.insertInto(
                DailyRecommendationTagTable.DAILY_RECOMMENDATION_TAGS,
                DailyRecommendationTagTable.DAILY_RECOMMENDATION_ID,
                DailyRecommendationTagTable.TAG_ID,
            )

        insertStep
            .valuesOfRows(rows)
            .onConflictDoNothing()
            .execute()
    }

    fun findByDailyRecommendationId(dailyRecommendationId: Long): List<DailyRecommendationTag> =
        dsl
            .select(DAILY_RECOMMENDATION_TAG_FIELDS)
            .from(DailyRecommendationTagTable.DAILY_RECOMMENDATION_TAGS)
            .where(DailyRecommendationTagTable.DAILY_RECOMMENDATION_ID.eq(dailyRecommendationId))
            .fetch(::toDailyRecommendationTag)

    private fun toDailyRecommendationTag(record: Record): DailyRecommendationTag =
        DailyRecommendationTag(
            id = record[DailyRecommendationTagTable.ID]!!,
            dailyRecommendationId = record[DailyRecommendationTagTable.DAILY_RECOMMENDATION_ID]!!,
            tagId = record[DailyRecommendationTagTable.TAG_ID]!!,
            createdAt = record[DailyRecommendationTagTable.CREATED_AT]!!,
        )

    private companion object {
        val DAILY_RECOMMENDATION_TAG_FIELDS: List<Field<*>> =
            listOf(
                DailyRecommendationTagTable.ID,
                DailyRecommendationTagTable.DAILY_RECOMMENDATION_ID,
                DailyRecommendationTagTable.TAG_ID,
                DailyRecommendationTagTable.CREATED_AT,
            )
    }
}
