package com.firstpenguin.app.domain.batch.repository

import com.firstpenguin.app.domain.batch.repository.table.QuoteMetadataBatchItemTable
import com.firstpenguin.app.domain.batch.repository.table.QuoteMetadataTable
import com.firstpenguin.app.domain.quote.model.Quote
import com.firstpenguin.app.domain.quote.repository.QuoteTable
import com.firstpenguin.app.global.enums.BatchItemStatus
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class QuoteMetadataRepository(
    private val dsl: DSLContext,
) {
    fun findPendingQuotes(limit: Int): List<Quote> =
        dsl
            .select(QUOTE_FIELDS)
            .from(QuoteTable.QUOTES)
            .where(QuoteTable.DELETED_AT.isNull)
            .and(metadataNotExists())
            .and(activeBatchItemNotExists())
            .orderBy(QuoteTable.ID.asc())
            .limit(limit)
            .fetch(::toQuote)

    private fun metadataNotExists() =
        DSL.notExists(
            DSL
                .selectOne()
                .from(QuoteMetadataTable.QUOTE_METADATA)
                .where(QuoteMetadataTable.QUOTE_ID.eq(QuoteTable.ID)),
        )

    private fun activeBatchItemNotExists() =
        DSL.notExists(
            DSL
                .selectOne()
                .from(QuoteMetadataBatchItemTable.QUOTE_METADATA_BATCH_ITEMS)
                .where(QuoteMetadataBatchItemTable.QUOTE_ID.eq(QuoteTable.ID))
                .and(
                    QuoteMetadataBatchItemTable.STATUS.`in`(
                        BatchItemStatus.activeStatuses().map { status -> status.name },
                    ),
                ),
        )

    private fun toQuote(record: Record): Quote =
        Quote(
            id = record[QuoteTable.ID]!!,
            bookId = record[QuoteTable.BOOK_ID]!!,
            content = record[QuoteTable.CONTENT]!!,
            createdAt = record[QuoteTable.CREATED_AT]!!,
            updatedAt = record[QuoteTable.UPDATED_AT]!!,
            deletedAt = record[QuoteTable.DELETED_AT],
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
