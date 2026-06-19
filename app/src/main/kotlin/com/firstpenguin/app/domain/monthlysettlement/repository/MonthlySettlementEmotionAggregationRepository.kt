package com.firstpenguin.app.domain.monthlysettlement.repository

import com.firstpenguin.app.domain.book.repository.BookTable
import com.firstpenguin.app.domain.emotion.repository.table.TagTable
import com.firstpenguin.app.domain.genre.repository.table.GenreTable
import com.firstpenguin.app.domain.monthlysettlement.model.MonthlySettlementEmotionTag
import com.firstpenguin.app.domain.monthlysettlement.model.MonthlySettlementSelectedBook
import com.firstpenguin.app.domain.quote.repository.QuoteTable
import com.firstpenguin.app.domain.quotemetadata.repository.table.QuoteMetadataTable
import com.firstpenguin.app.domain.quotemetadata.repository.table.QuoteMetadataTagTable
import com.firstpenguin.app.domain.recommendation.repository.table.RecommendationQuoteTable
import com.firstpenguin.app.domain.recommendation.repository.table.RecommendationTable
import com.firstpenguin.app.domain.recommendation.repository.table.RecommendationTagTable
import com.firstpenguin.app.global.enums.TagType
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class MonthlySettlementEmotionAggregationRepository(
    private val dsl: DSLContext,
) {
    fun findEmotionTagCounts(
        userId: Long,
        start: LocalDate,
        endExclusive: LocalDate,
        limit: Int,
    ): List<MonthlySettlementEmotionTag> {
        if (limit <= 0) return emptyList()

        return dsl
            .select(TagTable.ID, TagTable.EMOTION_RANGE_ID, TagTable.LABEL, TAG_COUNT)
            .from(RecommendationTagTable.RECOMMENDATION_TAGS)
            .join(RecommendationTable.RECOMMENDATIONS)
            .on(RecommendationTable.ID.eq(RecommendationTagTable.RECOMMENDATION_ID))
            .join(TagTable.TAGS)
            .on(TagTable.ID.eq(RecommendationTagTable.TAG_ID))
            .where(monthlyRecommendationCondition(userId, start, endExclusive))
            .and(TagTable.TYPE.eq(TagType.EMOTION.name))
            .groupBy(TagTable.ID, TagTable.EMOTION_RANGE_ID, TagTable.LABEL, TagTable.SORT_ORDER)
            .orderBy(TAG_COUNT.desc(), TagTable.SORT_ORDER.asc(), TagTable.ID.asc())
            .limit(limit)
            .fetch()
            .mapIndexed(::toEmotionTagCount)
    }

    fun findMonthlyBookCandidateByEmotionTagId(
        userId: Long,
        start: LocalDate,
        endExclusive: LocalDate,
        tagId: Long,
    ): MonthlySettlementSelectedBook? =
        findMonthlyBookCandidate(
            userId = userId,
            start = start,
            endExclusive = endExclusive,
            tagCondition = QuoteMetadataTagTable.TAG_ID.eq(tagId),
        )

    fun findMonthlyBookCandidateByEmotionRangeId(
        userId: Long,
        start: LocalDate,
        endExclusive: LocalDate,
        emotionRangeId: Long,
    ): MonthlySettlementSelectedBook? =
        findMonthlyBookCandidate(
            userId = userId,
            start = start,
            endExclusive = endExclusive,
            tagCondition = emotionRangeCondition(emotionRangeId),
        )

    private fun findMonthlyBookCandidate(
        userId: Long,
        start: LocalDate,
        endExclusive: LocalDate,
        tagCondition: Condition,
    ): MonthlySettlementSelectedBook? =
        dsl
            .select(MONTHLY_SELECTED_BOOK_FIELDS)
            .from(RecommendationQuoteTable.RECOMMENDATION_QUOTES)
            .join(RecommendationTable.RECOMMENDATIONS)
            .on(RecommendationTable.ID.eq(RecommendationQuoteTable.RECOMMENDATION_ID))
            .join(QuoteTable.QUOTES)
            .on(QuoteTable.ID.eq(RecommendationQuoteTable.QUOTE_ID))
            .join(QuoteMetadataTable.QUOTE_METADATA)
            .on(QuoteTable.ID.eq(QuoteMetadataTable.QUOTE_ID))
            .join(QuoteMetadataTagTable.QUOTE_METADATA_TAGS)
            .on(QuoteMetadataTable.ID.eq(QuoteMetadataTagTable.QUOTE_METADATA_ID))
            .join(TagTable.TAGS)
            .on(TagTable.ID.eq(QuoteMetadataTagTable.TAG_ID))
            .join(BookTable.BOOKS)
            .on(BookTable.ID.eq(QuoteTable.BOOK_ID))
            .join(GenreTable.GENRES)
            .on(GenreTable.ID.eq(BookTable.GENRE_ID))
            .where(monthlyRecommendationCondition(userId, start, endExclusive))
            .and(tagCondition)
            .and(activeQuoteAndBookCondition())
            .and(genreLabelExists())
            .groupBy(MONTHLY_SELECTED_BOOK_FIELDS)
            .orderBy(DSL.rand())
            .limit(1)
            .fetchOne(::toMonthlySelectedBook)

    private fun emotionRangeCondition(emotionRangeId: Long): Condition =
        TagTable.TYPE
            .eq(TagType.EMOTION.name)
            .and(TagTable.EMOTION_RANGE_ID.eq(emotionRangeId))

    private fun monthlyRecommendationCondition(
        userId: Long,
        start: LocalDate,
        endExclusive: LocalDate,
    ): Condition =
        RecommendationTable.USER_ID
            .eq(userId)
            .and(RecommendationTable.RECOMMENDATION_DATE.ge(start))
            .and(RecommendationTable.RECOMMENDATION_DATE.lt(endExclusive))

    private fun activeQuoteAndBookCondition(): Condition =
        QuoteTable.DELETED_AT
            .isNull
            .and(BookTable.DELETED_AT.isNull)

    private fun genreLabelExists(): Condition =
        GenreTable.LABEL
            .isNotNull
            .and(DSL.trim(GenreTable.LABEL).ne(""))

    private fun toEmotionTagCount(
        index: Int,
        record: Record,
    ): MonthlySettlementEmotionTag =
        MonthlySettlementEmotionTag(
            tagId = record[TagTable.ID]!!,
            emotionRangeId = record[TagTable.EMOTION_RANGE_ID],
            label = record[TagTable.LABEL]!!,
            count = record[TAG_COUNT]!!,
            sortOrder = index + 1,
        )

    private fun toMonthlySelectedBook(record: Record): MonthlySettlementSelectedBook =
        MonthlySettlementSelectedBook(
            quoteId = record[QuoteTable.ID]!!,
            bookId = record[BookTable.ID]!!,
            quoteContent = record[QuoteTable.CONTENT]!!,
            title = record[BookTable.TITLE]!!,
            author = record[BookTable.AUTHOR]!!,
            bookCoverImageUrl = record[BookTable.COVER_IMAGE_URL]!!,
            genre = record[GenreTable.LABEL]!!,
            bookPurchaseLink = record[BookTable.ALADIN_LINK]!!,
        )

    private companion object {
        val TAG_COUNT: Field<Int> = DSL.count(RecommendationTagTable.ID).`as`("tag_count")
        val MONTHLY_SELECTED_BOOK_FIELDS: List<Field<*>> =
            listOf(
                QuoteTable.ID,
                BookTable.ID,
                QuoteTable.CONTENT,
                BookTable.TITLE,
                BookTable.AUTHOR,
                BookTable.COVER_IMAGE_URL,
                GenreTable.LABEL,
                BookTable.ALADIN_LINK,
            )
    }
}
