package com.firstpenguin.app.domain.quote.repository

import com.firstpenguin.app.domain.book.repository.BookTable
import com.firstpenguin.app.domain.quote.model.QuoteScrapCursor
import com.firstpenguin.app.domain.quote.model.ScrappedQuote
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class QuoteScrapRepository(
    private val dsl: DSLContext,
) {
    fun insertIgnoreDuplicate(
        userId: Long,
        quoteId: Long,
    ): Int =
        dsl
            .insertInto(QuoteScrapTable.QUOTE_SCRAPS)
            .set(QuoteScrapTable.USER_ID, userId)
            .set(QuoteScrapTable.QUOTE_ID, quoteId)
            .onConflict(QuoteScrapTable.USER_ID, QuoteScrapTable.QUOTE_ID)
            .doNothing()
            .execute()

    fun deleteByUserIdAndQuoteId(
        userId: Long,
        quoteId: Long,
    ): Int =
        dsl
            .deleteFrom(QuoteScrapTable.QUOTE_SCRAPS)
            .where(QuoteScrapTable.USER_ID.eq(userId))
            .and(QuoteScrapTable.QUOTE_ID.eq(quoteId))
            .execute()

    fun deleteByUserIdAndQuoteIds(
        userId: Long,
        quoteIds: List<Long>,
    ): Int =
        dsl
            .deleteFrom(QuoteScrapTable.QUOTE_SCRAPS)
            .where(QuoteScrapTable.USER_ID.eq(userId))
            .and(QuoteScrapTable.QUOTE_ID.`in`(quoteIds))
            .execute()

    fun countActiveByUserId(userId: Long): Int =
        dsl.fetchCount(
            dsl
                .selectOne()
                .from(QuoteScrapTable.QUOTE_SCRAPS)
                .join(QuoteTable.QUOTES)
                .on(QuoteTable.ID.eq(QuoteScrapTable.QUOTE_ID))
                .join(BookTable.BOOKS)
                .on(BookTable.ID.eq(QuoteTable.BOOK_ID))
                .where(activeScrapCondition(userId)),
        )

    fun findActiveByUserId(
        userId: Long,
        cursor: QuoteScrapCursor?,
        limit: Int,
    ): List<ScrappedQuote> =
        dsl
            .select(SCRAPPED_QUOTE_FIELDS)
            .from(QuoteScrapTable.QUOTE_SCRAPS)
            .join(QuoteTable.QUOTES)
            .on(QuoteTable.ID.eq(QuoteScrapTable.QUOTE_ID))
            .join(BookTable.BOOKS)
            .on(BookTable.ID.eq(QuoteTable.BOOK_ID))
            .where(activeScrapCondition(userId))
            .and(cursor?.let(::cursorCondition) ?: DSL.noCondition())
            .orderBy(QuoteScrapTable.CREATED_AT.desc(), QuoteScrapTable.QUOTE_ID.desc())
            .limit(limit)
            .fetch(::toScrappedQuote)

    private fun activeScrapCondition(userId: Long): Condition =
        QuoteScrapTable.USER_ID
            .eq(userId)
            .and(QuoteTable.DELETED_AT.isNull)
            .and(BookTable.DELETED_AT.isNull)

    private fun cursorCondition(cursor: QuoteScrapCursor): Condition =
        QuoteScrapTable.CREATED_AT
            .lt(cursor.scrappedAt)
            .or(QuoteScrapTable.CREATED_AT.eq(cursor.scrappedAt).and(QuoteScrapTable.QUOTE_ID.lt(cursor.quoteId)))

    private fun toScrappedQuote(record: Record): ScrappedQuote =
        ScrappedQuote(
            quoteId = record.get(QuoteTable.ID),
            bookId = record.get(BookTable.ID),
            bookCoverImageUrl = record.get(BookTable.COVER_IMAGE_URL),
            content = record.get(QuoteTable.CONTENT),
            title = record.get(BookTable.TITLE),
            author = record.get(BookTable.AUTHOR),
            scrappedAt = record.get(QuoteScrapTable.CREATED_AT),
        )

    private companion object {
        val SCRAPPED_QUOTE_FIELDS: List<Field<*>> =
            listOf(
                QuoteTable.ID,
                BookTable.ID,
                BookTable.COVER_IMAGE_URL,
                QuoteTable.CONTENT,
                BookTable.TITLE,
                BookTable.AUTHOR,
                QuoteScrapTable.CREATED_AT,
            )
    }
}
