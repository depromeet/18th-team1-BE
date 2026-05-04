package com.firstpenguin.app.domain.book.repository

import com.firstpenguin.app.domain.book.model.Book
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.springframework.stereotype.Repository

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

    private fun toBook(record: Record): Book =
        Book(
            id = record.get(BookTable.ID),
            title = record.get(BookTable.TITLE),
            author = record.get(BookTable.AUTHOR),
            isbn13 = record.get(BookTable.ISBN13),
            aladinLink = record.get(BookTable.ALADIN_LINK),
            createdAt = record.get(BookTable.CREATED_AT),
            updatedAt = record.get(BookTable.UPDATED_AT),
            deletedAt = record.get(BookTable.DELETED_AT),
        )

    private companion object {
        val BOOK_FIELDS: List<Field<*>> =
            listOf(
                BookTable.ID,
                BookTable.TITLE,
                BookTable.AUTHOR,
                BookTable.ISBN13,
                BookTable.ALADIN_LINK,
                BookTable.CREATED_AT,
                BookTable.UPDATED_AT,
                BookTable.DELETED_AT,
            )
    }
}
