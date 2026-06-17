package com.firstpenguin.app.domain.discovery.repository

import com.firstpenguin.app.domain.book.repository.BookTable
import com.firstpenguin.app.domain.discovery.model.DiscoveryCursor
import com.firstpenguin.app.domain.discovery.model.DiscoveryGenre
import com.firstpenguin.app.domain.discovery.model.DiscoveryNeedTag
import com.firstpenguin.app.domain.discovery.model.DiscoveryQuote
import com.firstpenguin.app.domain.discovery.model.DiscoveryQuoteSearchCriteria
import com.firstpenguin.app.domain.discovery.model.DiscoveryQuoteSearchCursor
import com.firstpenguin.app.domain.discovery.model.DiscoveryQuoteSearchSort
import com.firstpenguin.app.domain.emotion.repository.table.TagTable
import com.firstpenguin.app.domain.quote.repository.QuoteScrapTable
import com.firstpenguin.app.domain.quote.repository.QuoteTable
import com.firstpenguin.app.domain.recommendation.repository.table.RecommendationQuoteTable
import com.firstpenguin.app.domain.recommendation.repository.table.RecommendationTable
import com.firstpenguin.app.domain.recommendation.repository.table.RecommendationTagTable
import com.firstpenguin.app.global.enums.TagType
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.SortField
import org.jooq.Table
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
@Suppress("TooManyFunctions")
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
        val needTags = recommendationNeedTags()
        val scrapCount = DSL.inline(0).`as`(SCRAP_COUNT)

        return baseDiscoveryQuery(userId, rankedRecommendationEvents, needTags, scrapCount)
            .where(latestRecommendedQuoteCondition(rankedRecommendationEvents, cursor))
            .and(activeQuoteCondition(genre))
            .orderBy(latestOrderBy(rankedRecommendationEvents))
            .limit(limit)
            .fetch(::toDiscoveryQuote)
    }

    fun searchRecommendedQuotes(criteria: DiscoveryQuoteSearchCriteria): List<DiscoveryQuote> {
        if (criteria.limit <= 0) return emptyList()

        val recommendationEvents = recommendationEvents()
        val rankedRecommendationEvents = rankedRecommendationEvents(recommendationEvents)
        val needTags = recommendationNeedTags()
        val quoteScrapCounts = quoteScrapCounts()
        val scrapCount = scrapCount(quoteScrapCounts)

        return baseDiscoverySearchQuery(
            criteria.userId,
            rankedRecommendationEvents,
            needTags,
            quoteScrapCounts,
            scrapCount,
        ).where(latestRecommendationRankCondition(rankedRecommendationEvents))
            .and(activeQuoteCondition(criteria.genre))
            .and(searchContentCondition(criteria.query))
            .and(searchCursorCondition(criteria.sort, rankedRecommendationEvents, scrapCount, criteria.cursor))
            .orderBy(searchOrderBy(criteria.sort, rankedRecommendationEvents, scrapCount))
            .limit(criteria.limit)
            .fetch(::toDiscoveryQuote)
    }

    private fun baseDiscoveryQuery(
        userId: Long,
        rankedRecommendationEvents: Table<*>,
        needTags: Table<*>,
        scrapCount: Field<Int>,
    ) = dsl
        .select(discoveryQuoteFields(rankedRecommendationEvents, needTags, scrapCount))
        .from(rankedRecommendationEvents)
        .join(QuoteTable.QUOTES)
        .on(QuoteTable.ID.eq(recommendedQuoteId(rankedRecommendationEvents)))
        .join(BookTable.BOOKS)
        .on(BookTable.ID.eq(QuoteTable.BOOK_ID))
        .leftJoin(QuoteScrapTable.QUOTE_SCRAPS)
        .on(QuoteScrapTable.QUOTE_ID.eq(QuoteTable.ID))
        .and(QuoteScrapTable.USER_ID.eq(userId))
        .leftJoin(needTags)
        .on(needTagRecommendationId(needTags).eq(recommendationId(rankedRecommendationEvents)))

    private fun baseDiscoverySearchQuery(
        userId: Long,
        rankedRecommendationEvents: Table<*>,
        needTags: Table<*>,
        quoteScrapCounts: Table<*>,
        scrapCount: Field<Int>,
    ) = baseDiscoveryQuery(userId, rankedRecommendationEvents, needTags, scrapCount)
        .leftJoin(quoteScrapCounts)
        .on(scrapCountQuoteId(quoteScrapCounts).eq(QuoteTable.ID))

    private fun latestRecommendedQuoteCondition(
        rankedRecommendationEvents: Table<*>,
        cursor: DiscoveryCursor?,
    ): Condition {
        val latestCondition = latestRecommendationRankCondition(rankedRecommendationEvents)
        if (cursor == null) return latestCondition

        return latestCondition.and(cursorCondition(rankedRecommendationEvents, cursor))
    }

    private fun latestRecommendationRankCondition(rankedRecommendationEvents: Table<*>): Condition =
        recommendationRank(rankedRecommendationEvents).eq(LATEST_RECOMMENDATION_RANK)

    private fun activeQuoteCondition(genre: DiscoveryGenre?): Condition =
        QuoteTable.DELETED_AT
            .isNull
            .and(BookTable.DELETED_AT.isNull)
            .and(genre?.let { selectedGenre -> BookTable.CATEGORY.eq(selectedGenre.value) } ?: DSL.noCondition())

    private fun searchContentCondition(query: String): Condition =
        DSL.condition(
            "{0} ilike {1} escape '!'",
            QuoteTable.CONTENT,
            DSL.value("%${escapeLikePattern(query)}%"),
        )

    private fun escapeLikePattern(query: String): String =
        query
            .replace(LIKE_ESCAPE, "$LIKE_ESCAPE$LIKE_ESCAPE")
            .replace("%", "$LIKE_ESCAPE%")
            .replace("_", "${LIKE_ESCAPE}_")

    private fun cursorCondition(
        rankedRecommendationEvents: Table<*>,
        cursor: DiscoveryCursor,
    ): Condition =
        at(rankedRecommendationEvents)
            .lt(cursor.recommendedAt)
            .or(at(rankedRecommendationEvents).eq(cursor.recommendedAt).and(QuoteTable.ID.lt(cursor.quoteId)))

    private fun searchCursorCondition(
        sort: DiscoveryQuoteSearchSort,
        rankedRecommendationEvents: Table<*>,
        scrapCount: Field<Int>,
        cursor: DiscoveryQuoteSearchCursor?,
    ): Condition {
        if (cursor == null) return DSL.noCondition()

        return when (sort) {
            DiscoveryQuoteSearchSort.LATEST -> {
                latestSearchCursorCondition(rankedRecommendationEvents, cursor)
            }

            DiscoveryQuoteSearchSort.SCRAP_COUNT -> {
                scrapCountCursorCondition(
                    rankedRecommendationEvents,
                    scrapCount,
                    cursor,
                )
            }
        }
    }

    private fun latestSearchCursorCondition(
        rankedRecommendationEvents: Table<*>,
        cursor: DiscoveryQuoteSearchCursor,
    ): Condition =
        cursorCondition(
            rankedRecommendationEvents = rankedRecommendationEvents,
            cursor = DiscoveryCursor(cursor.recommendedAt, cursor.quoteId),
        )

    private fun scrapCountCursorCondition(
        rankedRecommendationEvents: Table<*>,
        scrapCount: Field<Int>,
        cursor: DiscoveryQuoteSearchCursor,
    ): Condition =
        scrapCount
            .lt(requireNotNull(cursor.scrapCount))
            .or(
                scrapCount
                    .eq(cursor.scrapCount)
                    .and(scrapCountTieBreakerCondition(rankedRecommendationEvents, cursor)),
            )

    private fun scrapCountTieBreakerCondition(
        rankedRecommendationEvents: Table<*>,
        cursor: DiscoveryQuoteSearchCursor,
    ): Condition =
        at(rankedRecommendationEvents)
            .lt(cursor.recommendedAt)
            .or(at(rankedRecommendationEvents).eq(cursor.recommendedAt).and(QuoteTable.ID.lt(cursor.quoteId)))

    private fun latestOrderBy(rankedRecommendationEvents: Table<*>): List<SortField<*>> =
        listOf(at(rankedRecommendationEvents).desc(), QuoteTable.ID.desc())

    private fun searchOrderBy(
        sort: DiscoveryQuoteSearchSort,
        rankedRecommendationEvents: Table<*>,
        scrapCount: Field<Int>,
    ): List<SortField<*>> =
        when (sort) {
            DiscoveryQuoteSearchSort.LATEST -> latestOrderBy(rankedRecommendationEvents)
            DiscoveryQuoteSearchSort.SCRAP_COUNT -> scrapCountOrderBy(rankedRecommendationEvents, scrapCount)
        }

    private fun scrapCountOrderBy(
        rankedRecommendationEvents: Table<*>,
        scrapCount: Field<Int>,
    ): List<SortField<*>> =
        listOf(
            scrapCount.desc(),
            at(rankedRecommendationEvents).desc(),
            QuoteTable.ID.desc(),
        )

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
            needTag = toNeedTag(record),
            emotionValue = record.get(RECOMMENDED_EMOTION_VALUE_FIELD),
            recommendedAt = record.get(RECOMMENDED_AT_FIELD),
            isScrapped = record.get(IS_SCRAPPED_FIELD),
            scrapCount = record.get(SCRAP_COUNT_FIELD),
        )

    private fun toNeedTag(record: Record): DiscoveryNeedTag? =
        record.get(NEED_TAG_ID_FIELD)?.let { needTagId ->
            DiscoveryNeedTag(
                id = needTagId,
                label = record.get(NEED_TAG_LABEL_FIELD),
            )
        }

    private fun recommendationEvents(): Table<*> =
        DSL
            .select(
                RecommendationTable.ID.`as`(RECOMMENDATION_ID),
                RecommendationTable.QUOTE_ID.`as`(RECOMMENDED_QUOTE_ID),
                RecommendationTable.USER_ID.`as`(RECOMMENDED_USER_ID),
                RecommendationTable.EMOTION_VALUE.`as`(RECOMMENDED_EMOTION_VALUE),
                RecommendationTable.CREATED_AT.`as`(RECOMMENDED_AT),
            ).from(RecommendationTable.RECOMMENDATIONS)
            .unionAll(
                DSL
                    .select(
                        RecommendationQuoteTable.RECOMMENDATION_ID.`as`(RECOMMENDATION_ID),
                        RecommendationQuoteTable.QUOTE_ID.`as`(RECOMMENDED_QUOTE_ID),
                        RecommendationTable.USER_ID.`as`(RECOMMENDED_USER_ID),
                        RecommendationTable.EMOTION_VALUE.`as`(RECOMMENDED_EMOTION_VALUE),
                        RecommendationQuoteTable.CREATED_AT.`as`(RECOMMENDED_AT),
                    ).from(RecommendationQuoteTable.RECOMMENDATION_QUOTES)
                    .join(RecommendationTable.RECOMMENDATIONS)
                    .on(RecommendationTable.ID.eq(RecommendationQuoteTable.RECOMMENDATION_ID)),
            ).asTable(RECOMMENDATION_EVENTS)

    private fun rankedRecommendationEvents(recommendationEvents: Table<*>): Table<*> =
        DSL
            .select(
                recommendationId(recommendationEvents),
                recommendedQuoteId(recommendationEvents),
                recommendedUserId(recommendationEvents),
                recommendedEmotionValue(recommendationEvents),
                at(recommendationEvents),
                DSL
                    .rowNumber()
                    .over()
                    .partitionBy(recommendedQuoteId(recommendationEvents))
                    .orderBy(at(recommendationEvents).desc())
                    .`as`(RECOMMENDATION_RANK),
            ).from(recommendationEvents)
            .asTable(RANKED_RECOMMENDATION_EVENTS)

    private fun recommendationNeedTags(): Table<*> =
        DSL
            .select(
                RecommendationTagTable.RECOMMENDATION_ID.`as`(NEED_TAG_RECOMMENDATION_ID),
                TagTable.ID.`as`(NEED_TAG_ID),
                TagTable.LABEL.`as`(NEED_TAG_LABEL),
            ).from(RecommendationTagTable.RECOMMENDATION_TAGS)
            .join(TagTable.TAGS)
            .on(TagTable.ID.eq(RecommendationTagTable.TAG_ID))
            .where(TagTable.TYPE.eq(TagType.NEED.name))
            .asTable(RECOMMENDATION_NEED_TAGS)

    private fun quoteScrapCounts(): Table<*> =
        DSL
            .select(
                QuoteScrapTable.QUOTE_ID.`as`(SCRAP_COUNT_QUOTE_ID),
                DSL.count(QuoteScrapTable.ID).`as`(SCRAP_COUNT),
            ).from(QuoteScrapTable.QUOTE_SCRAPS)
            .groupBy(QuoteScrapTable.QUOTE_ID)
            .asTable(QUOTE_SCRAP_COUNTS)

    private fun discoveryQuoteFields(
        rankedRecommendationEvents: Table<*>,
        needTags: Table<*>,
        scrapCount: Field<Int>,
    ): List<Field<*>> =
        listOf(
            QuoteTable.ID,
            BookTable.ID,
            recommendedUserId(rankedRecommendationEvents),
            QuoteTable.CONTENT,
            BookTable.TITLE,
            BookTable.AUTHOR,
            BookTable.COVER_IMAGE_URL,
            BookTable.CATEGORY,
            needTagId(needTags),
            needTagLabel(needTags),
            recommendedEmotionValue(rankedRecommendationEvents),
            at(rankedRecommendationEvents),
            IS_SCRAPPED_FIELD,
            scrapCount,
        )

    private fun recommendationId(table: Table<*>): Field<Long> = table.field(RECOMMENDATION_ID, Long::class.java)!!

    private fun recommendedQuoteId(table: Table<*>): Field<Long> = table.field(RECOMMENDED_QUOTE_ID, Long::class.java)!!

    private fun recommendedUserId(table: Table<*>): Field<Long> = table.field(RECOMMENDED_USER_ID, Long::class.java)!!

    private fun recommendedEmotionValue(table: Table<*>): Field<Int> = table.field(RECOMMENDED_EMOTION_VALUE, Int::class.java)!!

    private fun at(table: Table<*>): Field<LocalDateTime> = table.field(RECOMMENDED_AT, LocalDateTime::class.java)!!

    private fun recommendationRank(table: Table<*>): Field<Int> = table.field(RECOMMENDATION_RANK, Int::class.java)!!

    private fun needTagRecommendationId(table: Table<*>): Field<Long> {
        val fieldName = NEED_TAG_RECOMMENDATION_ID
        return table.field(fieldName, Long::class.java)!!
    }

    private fun needTagId(table: Table<*>): Field<Long> = table.field(NEED_TAG_ID, Long::class.java)!!

    private fun needTagLabel(table: Table<*>): Field<String> = table.field(NEED_TAG_LABEL, String::class.java)!!

    private fun scrapCountQuoteId(table: Table<*>): Field<Long> = table.field(SCRAP_COUNT_QUOTE_ID, Long::class.java)!!

    private fun scrapCount(table: Table<*>): Field<Int> {
        val count = table.field(SCRAP_COUNT, Int::class.java)
        return DSL.coalesce(count, 0).`as`(SCRAP_COUNT)
    }

    private companion object {
        const val RECOMMENDATION_EVENTS = "recommendation_events"
        const val RANKED_RECOMMENDATION_EVENTS = "ranked_recommendation_events"
        const val RECOMMENDATION_NEED_TAGS = "recommendation_need_tags"
        const val QUOTE_SCRAP_COUNTS = "quote_scrap_counts"
        const val RECOMMENDATION_ID = "recommendation_id"
        const val RECOMMENDED_QUOTE_ID = "quote_id"
        const val RECOMMENDED_USER_ID = "recommended_user_id"
        const val RECOMMENDED_EMOTION_VALUE = "emotion_value"
        const val RECOMMENDED_AT = "recommended_at"
        const val RECOMMENDATION_RANK = "recommendation_rank"
        const val NEED_TAG_RECOMMENDATION_ID = "need_tag_recommendation_id"
        const val NEED_TAG_ID = "need_tag_id"
        const val NEED_TAG_LABEL = "need_tag_label"
        const val SCRAP_COUNT_QUOTE_ID = "scrap_count_quote_id"
        const val SCRAP_COUNT = "scrap_count"
        const val LIKE_ESCAPE = "!"
        const val LATEST_RECOMMENDATION_RANK = 1

        val RECOMMENDED_USER_ID_FIELD: Field<Long> = DSL.field(RECOMMENDED_USER_ID, Long::class.java)
        val RECOMMENDED_EMOTION_VALUE_FIELD: Field<Int> = DSL.field(RECOMMENDED_EMOTION_VALUE, Int::class.java)
        val RECOMMENDED_AT_FIELD: Field<LocalDateTime> = DSL.field(RECOMMENDED_AT, LocalDateTime::class.java)
        val NEED_TAG_ID_FIELD: Field<Long> = DSL.field(NEED_TAG_ID, Long::class.java)
        val NEED_TAG_LABEL_FIELD: Field<String> = DSL.field(NEED_TAG_LABEL, String::class.java)
        val IS_SCRAPPED_FIELD: Field<Boolean> = QuoteScrapTable.ID.isNotNull.`as`("is_scrapped")
        val SCRAP_COUNT_FIELD: Field<Int> = DSL.field(SCRAP_COUNT, Int::class.java)
    }
}
