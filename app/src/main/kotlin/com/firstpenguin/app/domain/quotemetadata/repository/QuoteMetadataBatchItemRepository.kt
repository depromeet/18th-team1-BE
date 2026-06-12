package com.firstpenguin.app.domain.quotemetadata.repository

import com.firstpenguin.app.domain.quote.repository.QuoteTable
import com.firstpenguin.app.domain.quotebatch.model.QuoteBatchType
import com.firstpenguin.app.domain.quotebatch.repository.table.QuoteBatchItemTable
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
    fun countActiveItemsWithoutMetadata(): Int = countItemsWithoutMetadata(BatchItemStatus.activeStatuses())

    fun countFailedItemsWithoutMetadata(): Int = countItemsWithoutMetadata(listOf(BatchItemStatus.FAILED))

    fun updateQuoteMetadataBatchItemsStatus(
        jobId: Long,
        status: BatchItemStatus,
        errorMessage: String? = null,
    ) = updateItemsStatus(jobId, status, errorMessage)

    fun updateQuoteMetadataBatchItemStatus(
        jobId: Long,
        customId: String,
        status: BatchItemStatus,
        errorMessage: String? = null,
    ) = updateItemStatus(jobId, customId, status, errorMessage)

    private fun countItemsWithoutMetadata(statuses: List<BatchItemStatus>): Int =
        dsl
            .select(DSL.countDistinct(QuoteBatchItemTable.TARGET_ID))
            .from(QuoteBatchItemTable.QUOTE_BATCH_ITEMS)
            .join(QuoteTable.QUOTES)
            .on(QuoteTable.ID.eq(QuoteBatchItemTable.TARGET_ID))
            .where(metadataItemCondition(statuses))
            .and(metadataNotExists())
            .fetchOne(0, Int::class.java) ?: 0

    private fun updateItemsStatus(
        jobId: Long,
        status: BatchItemStatus,
        errorMessage: String?,
    ) {
        dsl
            .update(QuoteBatchItemTable.QUOTE_BATCH_ITEMS)
            .set(QuoteBatchItemTable.STATUS, status.name)
            .set(QuoteBatchItemTable.ERROR_MESSAGE, errorMessage)
            .set(QuoteBatchItemTable.UPDATED_AT, LocalDateTime.now())
            .where(QuoteBatchItemTable.JOB_ID.eq(jobId))
            .execute()
    }

    private fun updateItemStatus(
        jobId: Long,
        customId: String,
        status: BatchItemStatus,
        errorMessage: String?,
    ) {
        dsl
            .update(QuoteBatchItemTable.QUOTE_BATCH_ITEMS)
            .set(QuoteBatchItemTable.STATUS, status.name)
            .set(QuoteBatchItemTable.ERROR_MESSAGE, errorMessage)
            .set(QuoteBatchItemTable.UPDATED_AT, LocalDateTime.now())
            .where(QuoteBatchItemTable.JOB_ID.eq(jobId))
            .and(QuoteBatchItemTable.CUSTOM_ID.eq(customId))
            .execute()
    }

    private fun metadataItemCondition(statuses: List<BatchItemStatus>) =
        QuoteTable.DELETED_AT.isNull
            .and(QuoteBatchItemTable.JOB_TYPE.eq(QuoteBatchType.QUOTE_METADATA.name))
            .and(QuoteBatchItemTable.STATUS.`in`(statuses.map { status -> status.name }))

    private fun metadataNotExists() =
        DSL.notExists(
            DSL
                .selectOne()
                .from(QuoteMetadataTable.QUOTE_METADATA)
                .where(QuoteMetadataTable.QUOTE_ID.eq(QuoteBatchItemTable.TARGET_ID)),
        )
}
