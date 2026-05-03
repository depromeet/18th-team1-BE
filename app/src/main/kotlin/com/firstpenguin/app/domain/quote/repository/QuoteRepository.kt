package com.firstpenguin.app.domain.quote.repository

import com.firstpenguin.app.domain.quote.model.Quote
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.impl.DSL.rand
import org.springframework.stereotype.Repository

@Repository
class QuoteRepository(
    private val dsl: DSLContext,
) {
    fun findRandomQuote(): Quote? =
        dsl
            .select(QUOTE_FIELDS)
            .from(QuoteTable.QUOTES)
            .where(QuoteTable.DELETED_AT.isNull)
            .orderBy(rand())
            .limit(1)
            .fetchOne(::toQuote)

    private fun toQuote(record: Record): Quote =
        Quote(
            id = record.get(QuoteTable.ID),
            bookId = record.get(QuoteTable.BOOK_ID),
            content = record.get(QuoteTable.CONTENT),
            createdAt = record.get(QuoteTable.CREATED_AT),
            updatedAt = record.get(QuoteTable.UPDATED_AT),
            deletedAt = record.get(QuoteTable.DELETED_AT),
        )

    private companion object {
        val QUOTE_FIELDS: List<Field<*>> =
            listOf(
                QuoteTable.ID,
                QuoteTable.BOOK_ID,
                QuoteTable.CONTENT,
                QuoteTable.CREATED_AT,
                QuoteTable.UPDATED_AT,
                QuoteTable.DELETED_AT,
            )
    }
}
