package com.firstpenguin.app.domain.quotemetadata.repository

import com.firstpenguin.app.domain.quote.repository.QuoteTable
import com.firstpenguin.app.domain.quotemetadata.repository.table.QuoteMetadataBatchItemTable
import com.firstpenguin.app.domain.quotemetadata.repository.table.QuoteMetadataTable
import com.firstpenguin.app.global.enums.BatchItemStatus
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class QuoteMetadataBatchItemRepository(
    private val dsl: DSLContext,
) {
    fun countActiveItemsWithoutMetadata(): Int = countItemsWithoutMetadata(statuses = BatchItemStatus.activeStatuses())

    fun countFailedItemsWithoutMetadata(): Int = countItemsWithoutMetadata(statuses = listOf(BatchItemStatus.FAILED))

    fun insertQuoteMetadataBatchItems(
        jobId: Long,
        quoteIds: List<Long>,
        status: BatchItemStatus,
    ) {
        if (quoteIds.isEmpty()) return

        val rows =
            quoteIds
                .distinct()
                .map { quoteId -> DSL.row(jobId, quoteId, status.name) }
        val insertStep =
            dsl.insertInto(
                QuoteMetadataBatchItemTable.QUOTE_METADATA_BATCH_ITEMS,
                QuoteMetadataBatchItemTable.JOB_ID,
                QuoteMetadataBatchItemTable.QUOTE_ID,
                QuoteMetadataBatchItemTable.STATUS,
            )

        insertStep
            .valuesOfRows(rows)
            .onConflictDoNothing()
            .execute()
    }

    fun updateQuoteMetadataBatchItemsStatus(
        jobId: Long,
        status: BatchItemStatus,
        errorMessage: String? = null,
    ) {
        dsl
            .update(QuoteMetadataBatchItemTable.QUOTE_METADATA_BATCH_ITEMS)
            .set(QuoteMetadataBatchItemTable.STATUS, status.name)
            .set(QuoteMetadataBatchItemTable.ERROR_MESSAGE, errorMessage)
            .set(QuoteMetadataBatchItemTable.UPDATED_AT, LocalDateTime.now())
            .where(QuoteMetadataBatchItemTable.JOB_ID.eq(jobId))
            .execute()
    }

    fun updateQuoteMetadataBatchItemStatus(
        jobId: Long,
        quoteId: Long,
        status: BatchItemStatus,
        errorMessage: String? = null,
    ) {
        dsl
            .update(QuoteMetadataBatchItemTable.QUOTE_METADATA_BATCH_ITEMS)
            .set(QuoteMetadataBatchItemTable.STATUS, status.name)
            .set(QuoteMetadataBatchItemTable.ERROR_MESSAGE, errorMessage)
            .set(QuoteMetadataBatchItemTable.UPDATED_AT, LocalDateTime.now())
            .where(QuoteMetadataBatchItemTable.JOB_ID.eq(jobId))
            .and(QuoteMetadataBatchItemTable.QUOTE_ID.eq(quoteId))
            .execute()
    }

    private fun countItemsWithoutMetadata(statuses: List<BatchItemStatus>): Int =
        dsl
            .select(DSL.countDistinct(QuoteMetadataBatchItemTable.QUOTE_ID))
            .from(QuoteMetadataBatchItemTable.QUOTE_METADATA_BATCH_ITEMS)
            .join(QuoteTable.QUOTES)
            .on(QuoteTable.ID.eq(QuoteMetadataBatchItemTable.QUOTE_ID))
            .where(QuoteTable.DELETED_AT.isNull)
            .and(QuoteMetadataBatchItemTable.STATUS.`in`(statuses.map { status -> status.name }))
            .and(metadataNotExists())
            .fetchOne(0, Int::class.java) ?: 0

    private fun metadataNotExists() =
        DSL.notExists(
            DSL
                .selectOne()
                .from(QuoteMetadataTable.QUOTE_METADATA)
                .where(QuoteMetadataTable.QUOTE_ID.eq(QuoteMetadataBatchItemTable.QUOTE_ID)),
        )
}
