package com.firstpenguin.app.domain.quote.repository

import com.firstpenguin.app.global.enums.QuoteConstants.RECOMMENDED_QUOTE_COUNT
import org.jooq.Condition
import org.jooq.Field
import org.jooq.Record1
import org.jooq.Select
import org.jooq.impl.DSL

fun activeQuoteCountLessThanRecommended(bookIdField: Field<Long?>): Condition =
    activeQuoteCount(bookIdField).lessThan(RECOMMENDED_QUOTE_COUNT)

fun activeQuoteCount(bookIdField: Field<Long?>): Field<Int> = DSL.field(activeQuoteCountSelect(bookIdField))

fun activeQuoteCountSelect(bookIdField: Field<Long?>): Select<Record1<Int>> =
    DSL
        .selectCount()
        .from(QuoteTable.QUOTES)
        .where(QuoteTable.BOOK_ID.eq(bookIdField))
        .and(QuoteTable.DELETED_AT.isNull)
