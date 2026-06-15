package com.firstpenguin.app.domain.monthlysettlement.repository

import com.firstpenguin.app.domain.monthlysettlement.model.MonthlySettlement
import com.firstpenguin.app.domain.monthlysettlement.model.MonthlySettlementBook
import com.firstpenguin.app.domain.monthlysettlement.model.MonthlySettlementEmotionTag
import com.firstpenguin.app.domain.monthlysettlement.model.MonthlySettlementSelectedBook
import com.firstpenguin.app.domain.monthlysettlement.repository.table.MonthlySettlementBookTable
import com.firstpenguin.app.domain.monthlysettlement.repository.table.MonthlySettlementEmotionTagTable
import com.firstpenguin.app.domain.monthlysettlement.repository.table.MonthlySettlementTable
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.springframework.stereotype.Repository

@Repository
class MonthlySettlementRepository(
    private val dsl: DSLContext,
) {
    fun findByUserIdAndYearMonth(
        userId: Long,
        year: Int,
        month: Int,
    ): MonthlySettlement? =
        dsl
            .select(MONTHLY_SETTLEMENT_FIELDS)
            .from(MonthlySettlementTable.MONTHLY_SETTLEMENTS)
            .where(MonthlySettlementTable.USER_ID.eq(userId))
            .and(MonthlySettlementTable.SETTLEMENT_YEAR.eq(year))
            .and(MonthlySettlementTable.SETTLEMENT_MONTH.eq(month))
            .fetchOne(::toMonthlySettlement)

    fun findEmotionTags(monthlySettlementId: Long): List<MonthlySettlementEmotionTag> =
        dsl
            .select(MONTHLY_SETTLEMENT_EMOTION_TAG_FIELDS)
            .from(MonthlySettlementEmotionTagTable.MONTHLY_SETTLEMENT_EMOTION_TAGS)
            .where(MonthlySettlementEmotionTagTable.MONTHLY_SETTLEMENT_ID.eq(monthlySettlementId))
            .orderBy(MonthlySettlementEmotionTagTable.SORT_ORDER.asc())
            .fetch(::toMonthlySettlementEmotionTag)

    fun findBooks(monthlySettlementId: Long): List<MonthlySettlementBook> =
        dsl
            .select(MONTHLY_SETTLEMENT_BOOK_FIELDS)
            .from(MonthlySettlementBookTable.MONTHLY_SETTLEMENT_BOOKS)
            .where(MonthlySettlementBookTable.MONTHLY_SETTLEMENT_ID.eq(monthlySettlementId))
            .orderBy(MonthlySettlementBookTable.SORT_ORDER.asc())
            .fetch(::toMonthlySettlementBook)

    private fun toMonthlySettlement(record: Record): MonthlySettlement =
        MonthlySettlement(
            id = record[MonthlySettlementTable.ID]!!,
            userId = record[MonthlySettlementTable.USER_ID]!!,
            year = record[MonthlySettlementTable.SETTLEMENT_YEAR]!!,
            month = record[MonthlySettlementTable.SETTLEMENT_MONTH]!!,
            sharedQuoteCount = record[MonthlySettlementTable.SHARED_QUOTE_COUNT]!!,
            mostFrequentGenre = record[MonthlySettlementTable.MOST_FREQUENT_GENRE],
            topEmotionTagId = record[MonthlySettlementTable.TOP_EMOTION_TAG_ID]!!,
            topEmotionTagLabel = record[MonthlySettlementTable.TOP_EMOTION_TAG_LABEL]!!,
            recommendationMessage = record[MonthlySettlementTable.RECOMMENDATION_MESSAGE]!!,
            monthlyBook = monthlySelectedBookRow(record).toMonthlySelectedBook(),
            createdAt = record[MonthlySettlementTable.CREATED_AT]!!,
        )

    private fun monthlySelectedBookRow(record: Record): MonthlySelectedBookRow =
        MonthlySelectedBookRow(
            quoteId = record[MonthlySettlementTable.SELECTED_QUOTE_ID],
            bookId = record[MonthlySettlementTable.SELECTED_BOOK_ID],
            quoteContent = record[MonthlySettlementTable.SELECTED_QUOTE_CONTENT],
            title = record[MonthlySettlementTable.SELECTED_BOOK_TITLE],
            author = record[MonthlySettlementTable.SELECTED_BOOK_AUTHOR],
            bookCoverImageUrl = record[MonthlySettlementTable.SELECTED_BOOK_COVER_IMAGE_URL],
            genre = record[MonthlySettlementTable.SELECTED_BOOK_GENRE],
        )

    private fun toMonthlySettlementEmotionTag(record: Record): MonthlySettlementEmotionTag =
        MonthlySettlementEmotionTag(
            tagId = record[MonthlySettlementEmotionTagTable.TAG_ID]!!,
            label = record[MonthlySettlementEmotionTagTable.TAG_LABEL]!!,
            count = record[MonthlySettlementEmotionTagTable.TAG_COUNT]!!,
            sortOrder = record[MonthlySettlementEmotionTagTable.SORT_ORDER]!!,
        )

    private fun toMonthlySettlementBook(record: Record): MonthlySettlementBook =
        MonthlySettlementBook(
            bookId = record[MonthlySettlementBookTable.BOOK_ID]!!,
            title = record[MonthlySettlementBookTable.TITLE]!!,
            author = record[MonthlySettlementBookTable.AUTHOR]!!,
            bookCoverImageUrl = record[MonthlySettlementBookTable.BOOK_COVER_IMAGE_URL]!!,
            genre = record[MonthlySettlementBookTable.GENRE]!!,
            sortOrder = record[MonthlySettlementBookTable.SORT_ORDER]!!,
        )

    private companion object {
        val MONTHLY_SETTLEMENT_FIELDS: List<Field<*>> =
            listOf(
                MonthlySettlementTable.ID,
                MonthlySettlementTable.USER_ID,
                MonthlySettlementTable.SETTLEMENT_YEAR,
                MonthlySettlementTable.SETTLEMENT_MONTH,
                MonthlySettlementTable.SHARED_QUOTE_COUNT,
                MonthlySettlementTable.MOST_FREQUENT_GENRE,
                MonthlySettlementTable.TOP_EMOTION_TAG_ID,
                MonthlySettlementTable.TOP_EMOTION_TAG_LABEL,
                MonthlySettlementTable.RECOMMENDATION_MESSAGE,
                MonthlySettlementTable.SELECTED_QUOTE_ID,
                MonthlySettlementTable.SELECTED_QUOTE_CONTENT,
                MonthlySettlementTable.SELECTED_BOOK_ID,
                MonthlySettlementTable.SELECTED_BOOK_TITLE,
                MonthlySettlementTable.SELECTED_BOOK_AUTHOR,
                MonthlySettlementTable.SELECTED_BOOK_COVER_IMAGE_URL,
                MonthlySettlementTable.SELECTED_BOOK_GENRE,
                MonthlySettlementTable.CREATED_AT,
            )
        val MONTHLY_SETTLEMENT_BOOK_FIELDS: List<Field<*>> =
            listOf(
                MonthlySettlementBookTable.BOOK_ID,
                MonthlySettlementBookTable.TITLE,
                MonthlySettlementBookTable.AUTHOR,
                MonthlySettlementBookTable.BOOK_COVER_IMAGE_URL,
                MonthlySettlementBookTable.GENRE,
                MonthlySettlementBookTable.SORT_ORDER,
            )
        val MONTHLY_SETTLEMENT_EMOTION_TAG_FIELDS: List<Field<*>> =
            listOf(
                MonthlySettlementEmotionTagTable.TAG_ID,
                MonthlySettlementEmotionTagTable.TAG_LABEL,
                MonthlySettlementEmotionTagTable.TAG_COUNT,
                MonthlySettlementEmotionTagTable.SORT_ORDER,
            )
    }
}

private data class MonthlySelectedBookRow(
    val quoteId: Long?,
    val bookId: Long?,
    val quoteContent: String?,
    val title: String?,
    val author: String?,
    val bookCoverImageUrl: String?,
    val genre: String?,
) {
    fun toMonthlySelectedBook(): MonthlySettlementSelectedBook? {
        val values = listOf(quoteId, bookId, quoteContent, title, author, bookCoverImageUrl, genre)

        if (values.any { value -> value == null }) {
            return null
        }

        return MonthlySettlementSelectedBook(
            quoteId = quoteId!!,
            bookId = bookId!!,
            quoteContent = quoteContent!!,
            title = title!!,
            author = author!!,
            bookCoverImageUrl = bookCoverImageUrl!!,
            genre = genre!!,
        )
    }
}
