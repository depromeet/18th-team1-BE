package com.firstpenguin.app.domain.quotemetadata.repository

import com.firstpenguin.app.domain.quote.model.Quote
import com.firstpenguin.app.domain.quote.repository.QuoteTable
import com.firstpenguin.app.domain.quotemetadata.model.QuoteMetadata
import com.firstpenguin.app.domain.quotemetadata.model.QuoteMetadataTag
import com.firstpenguin.app.domain.quotemetadata.repository.table.QuoteMetadataBatchItemTable
import com.firstpenguin.app.domain.quotemetadata.repository.table.QuoteMetadataTable
import com.firstpenguin.app.domain.quotemetadata.repository.table.QuoteMetadataTagTable
import com.firstpenguin.app.global.enums.BatchItemStatus
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class QuoteMetadataRepository(
    private val dsl: DSLContext,
) {
    fun countTotalQuotes(): Int =
        dsl
            .selectCount()
            .from(QuoteTable.QUOTES)
            .where(QuoteTable.DELETED_AT.isNull)
            .fetchOne(0, Int::class.java) ?: 0

    fun countCreatedMetadata(): Int =
        dsl
            .select(DSL.countDistinct(QuoteMetadataTable.QUOTE_ID))
            .from(QuoteMetadataTable.QUOTE_METADATA)
            .join(QuoteTable.QUOTES)
            .on(QuoteTable.ID.eq(QuoteMetadataTable.QUOTE_ID))
            .where(QuoteTable.DELETED_AT.isNull)
            .fetchOne(0, Int::class.java) ?: 0

    fun countPendingQuotes(): Int =
        dsl
            .selectCount()
            .from(QuoteTable.QUOTES)
            .where(QuoteTable.DELETED_AT.isNull)
            .and(metadataNotExists())
            .and(activeBatchItemNotExists())
            .fetchOne(0, Int::class.java) ?: 0

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

    fun upsertQuoteMetadata(quoteMetadata: QuoteMetadata): Long {
        val now = LocalDateTime.now()

        return dsl
            .insertInto(QuoteMetadataTable.QUOTE_METADATA)
            .set(QuoteMetadataTable.QUOTE_ID, quoteMetadata.quoteId)
            .set(QuoteMetadataTable.EMBEDDING_TEXT, quoteMetadata.embeddingText)
            .set(QuoteMetadataTable.METADATA_MODEL, quoteMetadata.metadataModel)
            .set(QuoteMetadataTable.METADATA_VERSION, quoteMetadata.metadataVersion)
            .set(QuoteMetadataTable.CREATED_AT, now)
            .set(QuoteMetadataTable.UPDATED_AT, now)
            .onConflict(QuoteMetadataTable.QUOTE_ID)
            .doUpdate()
            .set(QuoteMetadataTable.EMBEDDING_TEXT, quoteMetadata.embeddingText)
            .set(QuoteMetadataTable.METADATA_MODEL, quoteMetadata.metadataModel)
            .set(QuoteMetadataTable.METADATA_VERSION, quoteMetadata.metadataVersion)
            .set(QuoteMetadataTable.UPDATED_AT, now)
            .returningResult(QuoteMetadataTable.ID)
            .fetchOne(QuoteMetadataTable.ID)
            ?: throw CustomException(ErrorCode.INTERNAL_SERVER_ERROR)
    }

    fun replaceQuoteMetadataTags(
        quoteMetadataId: Long,
        quoteMetadataTags: List<QuoteMetadataTag>,
    ) {
        deleteStaleQuoteMetadataTags(
            quoteMetadataId = quoteMetadataId,
            tagIds = quoteMetadataTags.map { tag -> tag.tagId }.distinct(),
        )
        insertQuoteMetadataTags(quoteMetadataTags)
    }

    private fun deleteStaleQuoteMetadataTags(
        quoteMetadataId: Long,
        tagIds: List<Long>,
    ) {
        val condition =
            QuoteMetadataTagTable.QUOTE_METADATA_ID
                .eq(quoteMetadataId)
                .let { condition ->
                    if (tagIds.isEmpty()) {
                        condition
                    } else {
                        condition.and(QuoteMetadataTagTable.TAG_ID.notIn(tagIds))
                    }
                }

        dsl
            .deleteFrom(QuoteMetadataTagTable.QUOTE_METADATA_TAGS)
            .where(condition)
            .execute()
    }

    private fun insertQuoteMetadataTags(quoteMetadataTags: List<QuoteMetadataTag>) {
        if (quoteMetadataTags.isEmpty()) return

        val rows =
            quoteMetadataTags
                .distinctBy { tag -> tag.tagId }
                .map { tag -> DSL.row(tag.quoteMetadataId, tag.tagId) }

        dsl
            .insertInto(
                QuoteMetadataTagTable.QUOTE_METADATA_TAGS,
                QuoteMetadataTagTable.QUOTE_METADATA_ID,
                QuoteMetadataTagTable.TAG_ID,
            ).valuesOfRows(rows)
            .onConflictDoNothing()
            .execute()
    }

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
