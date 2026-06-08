package com.firstpenguin.app.domain.book.repository

import com.firstpenguin.app.domain.book.model.Book
import com.firstpenguin.app.domain.quote.repository.QuoteTable
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.Record1
import org.jooq.Select
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

private const val RECOMMENDED_QUOTE_COUNT = 3

@Repository
class BookRepository(
    private val dsl: DSLContext,
) {
    fun findBookById(id: Long): Book? =
        dsl
            .select(BOOK_FIELDS)
            .from(BookTable.BOOKS)
            .where(BookTable.ID.eq(id))
            .fetchOne(::toBook)

    fun countActiveBooks(): Int =
        dsl
            .selectCount()
            .from(BookTable.BOOKS)
            .where(BookTable.DELETED_AT.isNull)
            .fetchOne(0, Int::class.java) ?: 0

    fun countBooksWithRecommendedQuotes(): Int =
        dsl
            .selectCount()
            .from(BookTable.BOOKS)
            .where(BookTable.DELETED_AT.isNull)
            .and(activeQuoteCountGreaterOrEqualRecommended())
            .fetchOne(0, Int::class.java) ?: 0

    private fun toBook(record: Record): Book =
        Book(
            id = record.get(BookTable.ID),
            title = record.get(BookTable.TITLE),
            author = record.get(BookTable.AUTHOR),
            isbn13 = record.get(BookTable.ISBN13),
            aladinLink = record.get(BookTable.ALADIN_LINK),
            coverImageUrl = record.get(BookTable.COVER_IMAGE_URL),
            createdAt = record.get(BookTable.CREATED_AT),
            updatedAt = record.get(BookTable.UPDATED_AT),
            deletedAt = record.get(BookTable.DELETED_AT),
        )

    private fun activeQuoteCountGreaterOrEqualRecommended() = activeQuoteCount().greaterOrEqual(RECOMMENDED_QUOTE_COUNT)

    private fun activeQuoteCount(): Field<Int> = DSL.field(activeQuoteCountSelect())

    private fun activeQuoteCountSelect(): Select<Record1<Int>> =
        DSL
            .selectCount()
            .from(QuoteTable.QUOTES)
            .where(QuoteTable.BOOK_ID.eq(BookTable.ID))
            .and(QuoteTable.DELETED_AT.isNull)

    private companion object {
        val BOOK_FIELDS: List<Field<*>> =
            listOf(
                BookTable.ID,
                BookTable.TITLE,
                BookTable.AUTHOR,
                BookTable.ISBN13,
                BookTable.ALADIN_LINK,
                BookTable.COVER_IMAGE_URL,
                BookTable.CREATED_AT,
                BookTable.UPDATED_AT,
                BookTable.DELETED_AT,
            )
    }
}
