package com.firstpenguin.app.domain.quotebatch.repository

import com.firstpenguin.app.domain.quotebatch.model.QuoteBatchItem
import com.firstpenguin.app.domain.quotebatch.model.QuoteBatchType
import com.firstpenguin.app.domain.quotebatch.repository.table.QuoteBatchItemTable
import com.firstpenguin.app.global.enums.BatchItemStatus
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Query
import org.jooq.Record
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class QuoteBatchItemRepository(
    private val dsl: DSLContext,
) {
    fun insertQuoteBatchItems(
        jobId: Long,
        jobType: QuoteBatchType,
        targetIds: List<Long>,
        customIdPrefix: String,
        status: BatchItemStatus,
    ) {
        if (targetIds.isEmpty()) return
        dsl.insertRows(jobId, jobType, targetIds, customIdPrefix, status)
    }

    fun updateQuoteBatchItemsStatus(
        jobId: Long,
        status: BatchItemStatus,
        errorMessage: String? = null,
    ) {
        dsl
            .update(QuoteBatchItemTable.QUOTE_BATCH_ITEMS)
            .set(QuoteBatchItemTable.STATUS, status.name)
            .set(QuoteBatchItemTable.ERROR_MESSAGE, errorMessage)
            .set(QuoteBatchItemTable.UPDATED_AT, LocalDateTime.now())
            .where(QuoteBatchItemTable.JOB_ID.eq(jobId))
            .execute()
    }

    fun updateQuoteBatchItemStatusByTargetId(
        jobId: Long,
        targetId: Long,
        status: BatchItemStatus,
        errorMessage: String? = null,
    ) {
        dsl
            .update(QuoteBatchItemTable.QUOTE_BATCH_ITEMS)
            .set(QuoteBatchItemTable.STATUS, status.name)
            .set(QuoteBatchItemTable.ERROR_MESSAGE, errorMessage)
            .set(QuoteBatchItemTable.UPDATED_AT, LocalDateTime.now())
            .where(QuoteBatchItemTable.JOB_ID.eq(jobId))
            .and(QuoteBatchItemTable.TARGET_ID.eq(targetId))
            .execute()
    }

    fun findByJobIdAndTargetId(
        jobId: Long,
        targetId: Long,
    ): QuoteBatchItem? =
        dsl
            .select(QUOTE_BATCH_ITEM_FIELDS)
            .from(QuoteBatchItemTable.QUOTE_BATCH_ITEMS)
            .where(QuoteBatchItemTable.JOB_ID.eq(jobId))
            .and(QuoteBatchItemTable.TARGET_ID.eq(targetId))
            .fetchOne(::toQuoteBatchItem)

    fun countItemsByStatus(
        jobId: Long,
        status: BatchItemStatus,
    ): Int =
        dsl
            .selectCount()
            .from(QuoteBatchItemTable.QUOTE_BATCH_ITEMS)
            .where(QuoteBatchItemTable.JOB_ID.eq(jobId))
            .and(QuoteBatchItemTable.STATUS.eq(status.name))
            .fetchOne(0, Int::class.java) ?: 0

    fun countItems(
        jobTypes: List<QuoteBatchType>,
        statuses: List<BatchItemStatus>,
    ): Int =
        dsl
            .selectCount()
            .from(QuoteBatchItemTable.QUOTE_BATCH_ITEMS)
            .where(itemCondition(jobTypes, statuses))
            .fetchOne(0, Int::class.java) ?: 0

    private fun DSLContext.insertRows(
        jobId: Long,
        jobType: QuoteBatchType,
        targetIds: List<Long>,
        customIdPrefix: String,
        status: BatchItemStatus,
    ) {
        batch(targetIds.toInsertQueries(jobId, jobType, customIdPrefix, status)).execute()
    }

    private fun List<Long>.toInsertQueries(
        jobId: Long,
        jobType: QuoteBatchType,
        customIdPrefix: String,
        status: BatchItemStatus,
    ): List<Query> =
        distinct().map { targetId ->
            dsl.insertQuoteBatchItem(jobId, jobType, targetId, customIdPrefix, status)
        }

    private fun DSLContext.insertQuoteBatchItem(
        jobId: Long,
        jobType: QuoteBatchType,
        targetId: Long,
        customIdPrefix: String,
        status: BatchItemStatus,
    ): Query =
        insertInto(QuoteBatchItemTable.QUOTE_BATCH_ITEMS)
            .set(QuoteBatchItemTable.JOB_ID, jobId)
            .set(QuoteBatchItemTable.JOB_TYPE, jobType.name)
            .set(QuoteBatchItemTable.TARGET_ID, targetId)
            .set(QuoteBatchItemTable.CUSTOM_ID, "$customIdPrefix-$targetId")
            .set(QuoteBatchItemTable.STATUS, status.name)
            .onConflictDoNothing()

    private fun toQuoteBatchItem(record: Record): QuoteBatchItem =
        QuoteBatchItem(
            id = record[QuoteBatchItemTable.ID]!!,
            jobId = record[QuoteBatchItemTable.JOB_ID]!!,
            jobType = QuoteBatchType.valueOf(record[QuoteBatchItemTable.JOB_TYPE]!!),
            targetId = record[QuoteBatchItemTable.TARGET_ID]!!,
            customId = record[QuoteBatchItemTable.CUSTOM_ID]!!,
            status = BatchItemStatus.from(record[QuoteBatchItemTable.STATUS]!!),
            errorMessage = record[QuoteBatchItemTable.ERROR_MESSAGE],
            createdAt = record[QuoteBatchItemTable.CREATED_AT]!!,
            updatedAt = record[QuoteBatchItemTable.UPDATED_AT]!!,
        )

    private companion object {
        fun statusNames(statuses: List<BatchItemStatus>): List<String> = statuses.map { status -> status.name }

        fun jobTypeNames(jobTypes: List<QuoteBatchType>): List<String> = jobTypes.map { jobType -> jobType.name }

        fun itemCondition(
            jobTypes: List<QuoteBatchType>,
            statuses: List<BatchItemStatus>,
        ): Condition =
            QuoteBatchItemTable.JOB_TYPE
                .`in`(jobTypeNames(jobTypes))
                .and(QuoteBatchItemTable.STATUS.`in`(statusNames(statuses)))

        val QUOTE_BATCH_ITEM_FIELDS: List<Field<*>> =
            listOf(
                QuoteBatchItemTable.ID,
                QuoteBatchItemTable.JOB_ID,
                QuoteBatchItemTable.JOB_TYPE,
                QuoteBatchItemTable.TARGET_ID,
                QuoteBatchItemTable.CUSTOM_ID,
                QuoteBatchItemTable.STATUS,
                QuoteBatchItemTable.ERROR_MESSAGE,
                QuoteBatchItemTable.CREATED_AT,
                QuoteBatchItemTable.UPDATED_AT,
            )
    }
}
