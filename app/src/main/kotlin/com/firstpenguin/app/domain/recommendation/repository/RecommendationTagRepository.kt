package com.firstpenguin.app.domain.recommendation.repository

import com.firstpenguin.app.domain.recommendation.model.RecommendationTag
import com.firstpenguin.app.domain.recommendation.repository.table.RecommendationTagTable
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class RecommendationTagRepository(
    private val dsl: DSLContext,
) {
    fun insertRecommendationTag(
        recommendationId: Long,
        tagIds: List<Long>,
    ) {
        if (tagIds.isEmpty()) return

        val rows =
            tagIds
                .distinct()
                .map { tagId -> DSL.row(recommendationId, tagId) }
        val insertStep =
            dsl.insertInto(
                RecommendationTagTable.RECOMMENDATION_TAGS,
                RecommendationTagTable.RECOMMENDATION_ID,
                RecommendationTagTable.TAG_ID,
            )

        insertStep
            .valuesOfRows(rows)
            .onConflictDoNothing()
            .execute()
    }

    fun findByRecommendationId(recommendationId: Long): List<RecommendationTag> =
        dsl
            .select(RECOMMENDATION_TAG_FIELDS)
            .from(RecommendationTagTable.RECOMMENDATION_TAGS)
            .where(RecommendationTagTable.RECOMMENDATION_ID.eq(recommendationId))
            .fetch(::toRecommendationTag)

    fun deleteByRecommendationId(recommendationId: Long) {
        dsl
            .deleteFrom(RecommendationTagTable.RECOMMENDATION_TAGS)
            .where(RecommendationTagTable.RECOMMENDATION_ID.eq(recommendationId))
            .execute()
    }

    private fun toRecommendationTag(record: Record): RecommendationTag =
        RecommendationTag(
            id = record[RecommendationTagTable.ID]!!,
            recommendationId = record[RecommendationTagTable.RECOMMENDATION_ID]!!,
            tagId = record[RecommendationTagTable.TAG_ID]!!,
            createdAt = record[RecommendationTagTable.CREATED_AT]!!,
        )

    private companion object {
        val RECOMMENDATION_TAG_FIELDS: List<Field<*>> =
            listOf(
                RecommendationTagTable.ID,
                RecommendationTagTable.RECOMMENDATION_ID,
                RecommendationTagTable.TAG_ID,
                RecommendationTagTable.CREATED_AT,
            )
    }
}
