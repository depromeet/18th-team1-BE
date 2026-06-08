package com.firstpenguin.app.domain.quote.repository

import com.firstpenguin.app.domain.quote.model.Quote
import com.firstpenguin.app.domain.quote.model.QuoteSourceType
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.impl.DSL
import org.jooq.impl.DSL.max
import org.springframework.stereotype.Repository

@Repository
class QuoteRepository(
    private val dsl: DSLContext,
) {
    fun findQuoteById(id: Long?): Quote? =
        dsl
            .select(QUOTE_FIELDS)
            .from(QuoteTable.QUOTES)
            .where(QuoteTable.ID.eq(id))
            .and(QuoteTable.DELETED_AT.isNull)
            .fetchOne(::toQuote)

    fun getMaxQuoteId(): Long =
        dsl
            .select(DSL.coalesce(max(QuoteTable.ID), 0L))
            .from(QuoteTable.QUOTES)
            .fetchOne(0, Long::class.java)!!

    private fun toQuote(record: Record): Quote =
        Quote(
            id = record.get(QuoteTable.ID),
            bookId = record.get(QuoteTable.BOOK_ID),
            content = record.get(QuoteTable.CONTENT),
            sourceType = QuoteSourceType.valueOf(record.get(QuoteTable.SOURCE_TYPE)),
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
                QuoteTable.SOURCE_TYPE,
                QuoteTable.CREATED_AT,
                QuoteTable.UPDATED_AT,
                QuoteTable.DELETED_AT,
            )
    }
}
