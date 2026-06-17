package com.firstpenguin.app.domain.recommendation.repository

import com.firstpenguin.app.domain.recommendation.model.RecommendationQuote
import com.firstpenguin.app.domain.recommendation.repository.table.RecommendationQuoteTable
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.impl.DSL
import org.jooq.impl.DSL.max
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class RecommendationQuoteRepository(
    private val dsl: DSLContext,
) {
    fun insertRecommendationQuote(
        recommendationId: Long,
        quoteIds: List<Long>,
        nextDisplayOrder: Int,
    ) {
        if (quoteIds.isEmpty()) return

        val now = LocalDateTime.now()

        var insertStep =
            dsl.insertInto(
                RecommendationQuoteTable.RECOMMENDATION_QUOTES,
                RecommendationQuoteTable.RECOMMENDATION_ID,
                RecommendationQuoteTable.QUOTE_ID,
                RecommendationQuoteTable.DISPLAY_ORDER,
                RecommendationQuoteTable.CREATED_AT,
            )

        quoteIds.forEachIndexed { index, quoteId ->
            insertStep =
                insertStep.values(
                    recommendationId,
                    quoteId,
                    nextDisplayOrder + index,
                    now,
                )
        }

        insertStep.execute()
    }

    fun getMaxDisplayOrder(recommendationId: Long): Int =
        dsl
            .select(DSL.coalesce(max(RecommendationQuoteTable.DISPLAY_ORDER), 0))
            .from(RecommendationQuoteTable.RECOMMENDATION_QUOTES)
            .where(RecommendationQuoteTable.RECOMMENDATION_ID.eq(recommendationId))
            .fetchOne(0, Int::class.java)!!

    fun findByRecommendationId(recommendationId: Long): List<RecommendationQuote> =
        dsl
            .select(RECOMMENDATION_QUOTE_FIELDS)
            .from(RecommendationQuoteTable.RECOMMENDATION_QUOTES)
            .where(RecommendationQuoteTable.RECOMMENDATION_ID.eq(recommendationId))
            .orderBy(RecommendationQuoteTable.DISPLAY_ORDER.asc())
            .fetch(::toRecommendationQuote)

    fun existsByRecommendationIdAndQuoteId(
        recommendationId: Long,
        quoteId: Long,
    ): Boolean =
        dsl.fetchExists(
            RecommendationQuoteTable.RECOMMENDATION_QUOTES,
            RecommendationQuoteTable.RECOMMENDATION_ID
                .eq(recommendationId)
                .and(RecommendationQuoteTable.QUOTE_ID.eq(quoteId)),
        )

    fun deleteByRecommendationId(recommendationId: Long) {
        dsl
            .deleteFrom(RecommendationQuoteTable.RECOMMENDATION_QUOTES)
            .where(RecommendationQuoteTable.RECOMMENDATION_ID.eq(recommendationId))
            .execute()
    }

    private fun toRecommendationQuote(record: Record): RecommendationQuote =
        RecommendationQuote(
            id = record[RecommendationQuoteTable.ID]!!,
            recommendationId = record[RecommendationQuoteTable.RECOMMENDATION_ID]!!,
            quoteId = record[RecommendationQuoteTable.QUOTE_ID]!!,
            displayOrder = record[RecommendationQuoteTable.DISPLAY_ORDER]!!,
            createdAt = record[RecommendationQuoteTable.CREATED_AT]!!,
        )

    private companion object {
        val RECOMMENDATION_QUOTE_FIELDS: List<Field<*>> =
            listOf(
                RecommendationQuoteTable.ID,
                RecommendationQuoteTable.RECOMMENDATION_ID,
                RecommendationQuoteTable.QUOTE_ID,
                RecommendationQuoteTable.DISPLAY_ORDER,
                RecommendationQuoteTable.CREATED_AT,
            )
    }
}
