package com.firstpenguin.app.domain.batch.repository

import com.firstpenguin.app.domain.batch.repository.table.QuoteMetadataBatchItemTable
import com.firstpenguin.app.global.enums.BatchItemStatus
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class QuoteMetadataBatchItemRepository(
    private val dsl: DSLContext,
) {
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
}
