package com.firstpenguin.app.domain.recommendation.repository

import com.firstpenguin.app.domain.recommendation.model.DailyRecommendationQuote
import com.firstpenguin.app.domain.recommendation.repository.table.DailyRecommendationQuoteTable
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.impl.DSL
import org.jooq.impl.DSL.max
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class DailyRecommendationQuoteRepository(
    private val dsl: DSLContext,
) {
    fun insertDailyRecommendationQuote(
        dailyRecommendationId: Long,
        quoteIds: List<Long>,
        nextDisplayOrder: Int,
    ) {
        if (quoteIds.isEmpty()) return

        val now = LocalDateTime.now()

        var insertStep =
            dsl.insertInto(
                DailyRecommendationQuoteTable.DAILY_RECOMMENDATION_QUOTES,
                DailyRecommendationQuoteTable.DAILY_RECOMMENDATION_ID,
                DailyRecommendationQuoteTable.QUOTE_ID,
                DailyRecommendationQuoteTable.DISPLAY_ORDER,
                DailyRecommendationQuoteTable.CREATED_AT,
            )

        quoteIds.forEachIndexed { index, quoteId ->
            insertStep =
                insertStep.values(
                    dailyRecommendationId,
                    quoteId,
                    nextDisplayOrder + index,
                    now,
                )
        }

        insertStep.execute()
    }

    fun getMaxDisplayOrder(dailyRecommendationId: Long): Int =
        dsl
            .select(DSL.coalesce(max(DailyRecommendationQuoteTable.DISPLAY_ORDER), 0))
            .from(DailyRecommendationQuoteTable.DAILY_RECOMMENDATION_QUOTES)
            .where(DailyRecommendationQuoteTable.DAILY_RECOMMENDATION_ID.eq(dailyRecommendationId))
            .fetchOne(0, Int::class.java)!!

    fun findByDailyRecommendationId(dailyRecommendationId: Long): List<DailyRecommendationQuote> =
        dsl
            .select(DAILY_RECOMMENDATION_QUOTE_FIELDS)
            .from(DailyRecommendationQuoteTable.DAILY_RECOMMENDATION_QUOTES)
            .where(DailyRecommendationQuoteTable.DAILY_RECOMMENDATION_ID.eq(dailyRecommendationId))
            .fetch(::toDailyRecommendationQuote)

    private fun toDailyRecommendationQuote(record: Record): DailyRecommendationQuote =
        DailyRecommendationQuote(
            id = record[DailyRecommendationQuoteTable.ID]!!,
            dailyRecommendationId = record[DailyRecommendationQuoteTable.DAILY_RECOMMENDATION_ID]!!,
            quoteId = record[DailyRecommendationQuoteTable.QUOTE_ID]!!,
            displayOrder = record[DailyRecommendationQuoteTable.DISPLAY_ORDER]!!,
            createdAt = record[DailyRecommendationQuoteTable.CREATED_AT]!!,
        )

    private companion object {
        val DAILY_RECOMMENDATION_QUOTE_FIELDS: List<Field<*>> =
            listOf(
                DailyRecommendationQuoteTable.ID,
                DailyRecommendationQuoteTable.DAILY_RECOMMENDATION_ID,
                DailyRecommendationQuoteTable.QUOTE_ID,
                DailyRecommendationQuoteTable.DISPLAY_ORDER,
                DailyRecommendationQuoteTable.CREATED_AT,
            )
    }
}
