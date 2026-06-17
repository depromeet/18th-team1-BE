package com.firstpenguin.app.domain.monthlysettlement.repository

import com.firstpenguin.app.domain.book.repository.BookTable
import com.firstpenguin.app.domain.monthlysettlement.model.MonthlySettlementBook
import com.firstpenguin.app.domain.quote.repository.QuoteTable
import com.firstpenguin.app.domain.recommendation.repository.table.RecommendationQuoteTable
import com.firstpenguin.app.domain.recommendation.repository.table.RecommendationTable
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class MonthlySettlementQuoteAggregationRepository(
    private val dsl: DSLContext,
) {
    fun countRecommendedQuotes(
        userId: Long,
        start: LocalDate,
        endExclusive: LocalDate,
    ): Int =
        dsl
            .selectCount()
            .from(RecommendationQuoteTable.RECOMMENDATION_QUOTES)
            .join(RecommendationTable.RECOMMENDATIONS)
            .on(RecommendationTable.ID.eq(RecommendationQuoteTable.RECOMMENDATION_ID))
            .where(monthlyRecommendationCondition(userId, start, endExclusive))
            .fetchOne(0, Int::class.java) ?: 0

    fun findMostFrequentGenre(
        userId: Long,
        start: LocalDate,
        endExclusive: LocalDate,
    ): String? =
        dsl
            .select(BookTable.CATEGORY)
            .from(RecommendationQuoteTable.RECOMMENDATION_QUOTES)
            .join(RecommendationTable.RECOMMENDATIONS)
            .on(RecommendationTable.ID.eq(RecommendationQuoteTable.RECOMMENDATION_ID))
            .join(QuoteTable.QUOTES)
            .on(QuoteTable.ID.eq(RecommendationQuoteTable.QUOTE_ID))
            .join(BookTable.BOOKS)
            .on(BookTable.ID.eq(QuoteTable.BOOK_ID))
            .where(monthlyRecommendationCondition(userId, start, endExclusive))
            .and(activeQuoteAndBookCondition())
            .and(bookGenreExists())
            .groupBy(BookTable.CATEGORY)
            .orderBy(DSL.count(RecommendationQuoteTable.ID).desc(), BookTable.CATEGORY.asc())
            .limit(1)
            .fetchOne(BookTable.CATEGORY)

    fun findRecommendedBooksByGenre(
        userId: Long,
        start: LocalDate,
        endExclusive: LocalDate,
        genre: String,
        limit: Int,
    ): List<MonthlySettlementBook> {
        if (limit <= 0) return emptyList()

        return dsl
            .select(MONTHLY_BOOK_FIELDS)
            .from(RecommendationQuoteTable.RECOMMENDATION_QUOTES)
            .join(RecommendationTable.RECOMMENDATIONS)
            .on(RecommendationTable.ID.eq(RecommendationQuoteTable.RECOMMENDATION_ID))
            .join(QuoteTable.QUOTES)
            .on(QuoteTable.ID.eq(RecommendationQuoteTable.QUOTE_ID))
            .join(BookTable.BOOKS)
            .on(BookTable.ID.eq(QuoteTable.BOOK_ID))
            .where(monthlyRecommendationCondition(userId, start, endExclusive))
            .and(activeQuoteAndBookCondition())
            .and(BookTable.CATEGORY.eq(genre))
            .groupBy(MONTHLY_BOOK_FIELDS)
            .orderBy(DSL.count(RecommendationQuoteTable.ID).desc(), BookTable.ID.asc())
            .limit(limit)
            .fetch(::toMonthlyBookCandidate)
    }

    fun findFallbackBooksByGenre(
        genre: String,
        excludedBookIds: List<Long>,
        limit: Int,
    ): List<MonthlySettlementBook> {
        if (limit <= 0) return emptyList()

        return dsl
            .select(MONTHLY_BOOK_FIELDS)
            .from(BookTable.BOOKS)
            .where(BookTable.DELETED_AT.isNull)
            .and(BookTable.CATEGORY.eq(genre))
            .and(excludedBookCondition(excludedBookIds))
            .orderBy(BookTable.ID.asc())
            .limit(limit)
            .fetch(::toMonthlyBookCandidate)
    }

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

    private fun bookGenreExists(): Condition =
        BookTable.CATEGORY
            .isNotNull
            .and(DSL.trim(BookTable.CATEGORY).ne(""))

    private fun excludedBookCondition(excludedBookIds: List<Long>): Condition {
        if (excludedBookIds.isEmpty()) return DSL.noCondition()

        return BookTable.ID.notIn(excludedBookIds)
    }

    private fun toMonthlyBookCandidate(record: Record): MonthlySettlementBook =
        MonthlySettlementBook(
            bookId = record[BookTable.ID]!!,
            title = record[BookTable.TITLE]!!,
            author = record[BookTable.AUTHOR]!!,
            bookCoverImageUrl = record[BookTable.COVER_IMAGE_URL]!!,
            genre = record[BookTable.CATEGORY]!!,
            sortOrder = 0,
        )

    private companion object {
        val MONTHLY_BOOK_FIELDS: List<Field<*>> =
            listOf(
                BookTable.ID,
                BookTable.TITLE,
                BookTable.AUTHOR,
                BookTable.COVER_IMAGE_URL,
                BookTable.CATEGORY,
            )
    }
}
