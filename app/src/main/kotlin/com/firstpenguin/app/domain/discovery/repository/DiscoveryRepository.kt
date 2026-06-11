package com.firstpenguin.app.domain.discovery.repository

import com.firstpenguin.app.domain.book.repository.BookTable
import com.firstpenguin.app.domain.discovery.model.DiscoveryCursor
import com.firstpenguin.app.domain.discovery.model.DiscoveryGenre
import com.firstpenguin.app.domain.discovery.model.DiscoveryQuote
import com.firstpenguin.app.domain.quote.repository.QuoteScrapTable
import com.firstpenguin.app.domain.quote.repository.QuoteTable
import com.firstpenguin.app.domain.recommendation.repository.table.DailyRecommendationQuoteTable
import com.firstpenguin.app.domain.recommendation.repository.table.DailyRecommendationTable
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.Table
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class DiscoveryRepository(
    private val dsl: DSLContext,
) {
    fun findRecommendedQuotes(
        userId: Long,
        cursor: DiscoveryCursor?,
        genre: DiscoveryGenre?,
        limit: Int,
    ): List<DiscoveryQuote> {
        if (limit <= 0) return emptyList()

        val recommendationEvents = recommendationEvents()
        val rankedRecommendationEvents = rankedRecommendationEvents(recommendationEvents)
        val recommendedQuoteId = recommendedQuoteId(rankedRecommendationEvents)

        return dsl
            .select(discoveryQuoteFields(rankedRecommendationEvents))
            .from(rankedRecommendationEvents)
            .join(QuoteTable.QUOTES)
            .on(QuoteTable.ID.eq(recommendedQuoteId))
            .join(BookTable.BOOKS)
            .on(BookTable.ID.eq(QuoteTable.BOOK_ID))
            .leftJoin(QuoteScrapTable.QUOTE_SCRAPS)
            .on(QuoteScrapTable.QUOTE_ID.eq(QuoteTable.ID))
            .and(QuoteScrapTable.USER_ID.eq(userId))
            .where(latestRecommendedQuoteCondition(rankedRecommendationEvents, cursor))
            .and(QuoteTable.DELETED_AT.isNull)
            .and(BookTable.DELETED_AT.isNull)
            .and(genre?.let { selectedGenre -> BookTable.CATEGORY.eq(selectedGenre.value) } ?: DSL.noCondition())
            .orderBy(at(rankedRecommendationEvents).desc(), QuoteTable.ID.desc())
            .limit(limit)
            .fetch(::toDiscoveryQuote)
    }

    private fun latestRecommendedQuoteCondition(
        rankedRecommendationEvents: Table<*>,
        cursor: DiscoveryCursor?,
    ): Condition {
        val latestCondition = recommendationRank(rankedRecommendationEvents).eq(LATEST_RECOMMENDATION_RANK)
        if (cursor == null) return latestCondition

        return latestCondition.and(cursorCondition(rankedRecommendationEvents, cursor))
    }

    private fun cursorCondition(
        rankedRecommendationEvents: Table<*>,
        cursor: DiscoveryCursor,
    ): Condition =
        at(rankedRecommendationEvents)
            .lt(cursor.recommendedAt)
            .or(at(rankedRecommendationEvents).eq(cursor.recommendedAt).and(QuoteTable.ID.lt(cursor.quoteId)))

    private fun toDiscoveryQuote(record: Record): DiscoveryQuote =
        DiscoveryQuote(
            quoteId = record.get(QuoteTable.ID),
            bookId = record.get(BookTable.ID),
            recommendedUserId = record.get(RECOMMENDED_USER_ID_FIELD),
            content = record.get(QuoteTable.CONTENT),
            title = record.get(BookTable.TITLE),
            author = record.get(BookTable.AUTHOR),
            bookCoverImageUrl = record.get(BookTable.COVER_IMAGE_URL),
            genre = record.get(BookTable.CATEGORY),
            recommendedAt = record.get(RECOMMENDED_AT_FIELD),
            isScrapped = record.get(IS_SCRAPPED_FIELD),
        )

    private fun recommendationEvents(): Table<*> =
        DSL
            .select(
                DailyRecommendationTable.QUOTE_ID.`as`(RECOMMENDED_QUOTE_ID),
                DailyRecommendationTable.USER_ID.`as`(RECOMMENDED_USER_ID),
                DailyRecommendationTable.CREATED_AT.`as`(RECOMMENDED_AT),
            ).from(DailyRecommendationTable.DAILY_RECOMMENDATIONS)
            .unionAll(
                DSL
                    .select(
                        DailyRecommendationQuoteTable.QUOTE_ID.`as`(RECOMMENDED_QUOTE_ID),
                        DailyRecommendationTable.USER_ID.`as`(RECOMMENDED_USER_ID),
                        DailyRecommendationQuoteTable.CREATED_AT.`as`(RECOMMENDED_AT),
                    ).from(DailyRecommendationQuoteTable.DAILY_RECOMMENDATION_QUOTES)
                    .join(DailyRecommendationTable.DAILY_RECOMMENDATIONS)
                    .on(DailyRecommendationTable.ID.eq(DailyRecommendationQuoteTable.DAILY_RECOMMENDATION_ID)),
            ).asTable(RECOMMENDATION_EVENTS)

    private fun rankedRecommendationEvents(recommendationEvents: Table<*>): Table<*> =
        DSL
            .select(
                recommendedQuoteId(recommendationEvents),
                recommendedUserId(recommendationEvents),
                at(recommendationEvents),
                DSL
                    .rowNumber()
                    .over()
                    .partitionBy(recommendedQuoteId(recommendationEvents))
                    .orderBy(at(recommendationEvents).desc())
                    .`as`(RECOMMENDATION_RANK),
            ).from(recommendationEvents)
            .asTable(RANKED_RECOMMENDATION_EVENTS)

    private fun discoveryQuoteFields(rankedRecommendationEvents: Table<*>): List<Field<*>> =
        listOf(
            QuoteTable.ID,
            BookTable.ID,
            recommendedUserId(rankedRecommendationEvents),
            QuoteTable.CONTENT,
            BookTable.TITLE,
            BookTable.AUTHOR,
            BookTable.COVER_IMAGE_URL,
            BookTable.CATEGORY,
            at(rankedRecommendationEvents),
            IS_SCRAPPED_FIELD,
        )

    private fun recommendedQuoteId(table: Table<*>): Field<Long> = table.field(RECOMMENDED_QUOTE_ID, Long::class.java)!!

    private fun recommendedUserId(table: Table<*>): Field<Long> = table.field(RECOMMENDED_USER_ID, Long::class.java)!!

    private fun at(table: Table<*>): Field<LocalDateTime> = table.field(RECOMMENDED_AT, LocalDateTime::class.java)!!

    private fun recommendationRank(table: Table<*>): Field<Int> = table.field(RECOMMENDATION_RANK, Int::class.java)!!

    private companion object {
        const val RECOMMENDATION_EVENTS = "recommendation_events"
        const val RANKED_RECOMMENDATION_EVENTS = "ranked_recommendation_events"
        const val RECOMMENDED_QUOTE_ID = "quote_id"
        const val RECOMMENDED_USER_ID = "recommended_user_id"
        const val RECOMMENDED_AT = "recommended_at"
        const val RECOMMENDATION_RANK = "recommendation_rank"
        const val LATEST_RECOMMENDATION_RANK = 1

        val RECOMMENDED_USER_ID_FIELD: Field<Long> = DSL.field(RECOMMENDED_USER_ID, Long::class.java)
        val RECOMMENDED_AT_FIELD: Field<LocalDateTime> = DSL.field(RECOMMENDED_AT, LocalDateTime::class.java)
        val IS_SCRAPPED_FIELD: Field<Boolean> = QuoteScrapTable.ID.isNotNull.`as`("is_scrapped")
    }
}
