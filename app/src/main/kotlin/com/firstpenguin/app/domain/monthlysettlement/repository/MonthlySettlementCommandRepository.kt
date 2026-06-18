package com.firstpenguin.app.domain.monthlysettlement.repository

import com.firstpenguin.app.domain.monthlysettlement.model.MonthlySettlementBook
import com.firstpenguin.app.domain.monthlysettlement.model.MonthlySettlementCreateCommand
import com.firstpenguin.app.domain.monthlysettlement.model.MonthlySettlementEmotionTag
import com.firstpenguin.app.domain.monthlysettlement.repository.table.MonthlySettlementBookTable
import com.firstpenguin.app.domain.monthlysettlement.repository.table.MonthlySettlementEmotionTagTable
import com.firstpenguin.app.domain.monthlysettlement.repository.table.MonthlySettlementTable
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class MonthlySettlementCommandRepository(
    private val dsl: DSLContext,
) {
    fun insertMonthlySettlement(command: MonthlySettlementCreateCommand): Long? {
        val monthlySettlementId = insertMonthlySettlementRow(command) ?: return null

        insertMonthlySettlementBooks(monthlySettlementId, command.monthlyBooks)
        insertMonthlySettlementEmotionTags(monthlySettlementId, command.emotionTags)

        return monthlySettlementId
    }

    private fun insertMonthlySettlementRow(command: MonthlySettlementCreateCommand): Long? =
        dsl
            .insertInto(MonthlySettlementTable.MONTHLY_SETTLEMENTS)
            .set(MonthlySettlementTable.USER_ID, command.userId)
            .set(MonthlySettlementTable.SETTLEMENT_YEAR, command.year)
            .set(MonthlySettlementTable.SETTLEMENT_MONTH, command.month)
            .set(MonthlySettlementTable.SHARED_QUOTE_COUNT, command.sharedQuoteCount)
            .set(MonthlySettlementTable.MOST_FREQUENT_GENRE, command.mostFrequentGenre)
            .set(MonthlySettlementTable.TOP_EMOTION_TAG_ID, command.topEmotionTag.tagId)
            .set(MonthlySettlementTable.TOP_EMOTION_TAG_LABEL, command.topEmotionTag.label)
            .set(MonthlySettlementTable.RECOMMENDATION_MESSAGE, command.recommendationMessage)
            .set(MonthlySettlementTable.SELECTED_QUOTE_ID, command.monthlyBook?.quoteId)
            .set(MonthlySettlementTable.SELECTED_QUOTE_CONTENT, command.monthlyBook?.quoteContent)
            .set(MonthlySettlementTable.SELECTED_BOOK_ID, command.monthlyBook?.bookId)
            .set(MonthlySettlementTable.SELECTED_BOOK_TITLE, command.monthlyBook?.title)
            .set(MonthlySettlementTable.SELECTED_BOOK_AUTHOR, command.monthlyBook?.author)
            .set(MonthlySettlementTable.SELECTED_BOOK_COVER_IMAGE_URL, command.monthlyBook?.bookCoverImageUrl)
            .set(MonthlySettlementTable.SELECTED_BOOK_GENRE, command.monthlyBook?.genre)
            .set(MonthlySettlementTable.SELECTED_BOOK_PURCHASE_LINK, command.monthlyBook?.bookPurchaseLink)
            .onConflict(
                MonthlySettlementTable.USER_ID,
                MonthlySettlementTable.SETTLEMENT_YEAR,
                MonthlySettlementTable.SETTLEMENT_MONTH,
            ).doNothing()
            .returningResult(MonthlySettlementTable.ID)
            .fetchOne(MonthlySettlementTable.ID)

    private fun insertMonthlySettlementBooks(
        monthlySettlementId: Long,
        books: List<MonthlySettlementBook>,
    ) {
        if (books.isEmpty()) return

        val rows =
            books.map { book ->
                DSL.row(
                    monthlySettlementId,
                    book.bookId,
                    book.title,
                    book.author,
                    book.bookCoverImageUrl,
                    book.genre,
                    book.sortOrder,
                )
            }

        dsl
            .insertInto(
                MonthlySettlementBookTable.MONTHLY_SETTLEMENT_BOOKS,
                MonthlySettlementBookTable.MONTHLY_SETTLEMENT_ID,
                MonthlySettlementBookTable.BOOK_ID,
                MonthlySettlementBookTable.TITLE,
                MonthlySettlementBookTable.AUTHOR,
                MonthlySettlementBookTable.BOOK_COVER_IMAGE_URL,
                MonthlySettlementBookTable.GENRE,
                MonthlySettlementBookTable.SORT_ORDER,
            ).valuesOfRows(rows)
            .execute()
    }

    private fun insertMonthlySettlementEmotionTags(
        monthlySettlementId: Long,
        emotionTags: List<MonthlySettlementEmotionTag>,
    ) {
        if (emotionTags.isEmpty()) return

        val rows =
            emotionTags.map { emotionTag ->
                DSL.row(
                    monthlySettlementId,
                    emotionTag.tagId,
                    emotionTag.label,
                    emotionTag.count,
                    emotionTag.sortOrder,
                )
            }

        dsl
            .insertInto(
                MonthlySettlementEmotionTagTable.MONTHLY_SETTLEMENT_EMOTION_TAGS,
                MonthlySettlementEmotionTagTable.MONTHLY_SETTLEMENT_ID,
                MonthlySettlementEmotionTagTable.TAG_ID,
                MonthlySettlementEmotionTagTable.TAG_LABEL,
                MonthlySettlementEmotionTagTable.TAG_COUNT,
                MonthlySettlementEmotionTagTable.SORT_ORDER,
            ).valuesOfRows(rows)
            .execute()
    }
}
