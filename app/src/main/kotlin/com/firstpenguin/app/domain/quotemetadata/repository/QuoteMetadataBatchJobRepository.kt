package com.firstpenguin.app.domain.quotemetadata.repository

import com.firstpenguin.app.domain.quotemetadata.model.QuoteMetadataBatchJob
import com.firstpenguin.app.domain.quotemetadata.repository.table.QuoteMetadataBatchJobTable
import com.firstpenguin.app.global.enums.BatchJobStatus
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class QuoteMetadataBatchJobRepository(
    private val dsl: DSLContext,
) {
    fun insertPreparingQuoteMetadataBatchJob(
        metadataModel: String,
        metadataVersion: Int,
        submittedCount: Int,
    ): Long {
        val now = LocalDateTime.now()

        return dsl
            .insertInto(QuoteMetadataBatchJobTable.QUOTE_METADATA_BATCH_JOBS)
            .set(QuoteMetadataBatchJobTable.STATUS, BatchJobStatus.PREPARING.name)
            .set(QuoteMetadataBatchJobTable.METADATA_MODEL, metadataModel)
            .set(QuoteMetadataBatchJobTable.METADATA_VERSION, metadataVersion)
            .set(QuoteMetadataBatchJobTable.SUBMITTED_COUNT, submittedCount)
            .set(QuoteMetadataBatchJobTable.CREATED_AT, now)
            .set(QuoteMetadataBatchJobTable.UPDATED_AT, now)
            .returningResult(QuoteMetadataBatchJobTable.ID)
            .fetchOne(QuoteMetadataBatchJobTable.ID)
            ?: throw CustomException(ErrorCode.INTERNAL_SERVER_ERROR)
    }

    fun updateQuoteMetadataBatchJobAsSubmitted(
        jobId: Long,
        openAiBatchId: String,
        inputFileId: String,
        status: BatchJobStatus,
    ) {
        dsl
            .update(QuoteMetadataBatchJobTable.QUOTE_METADATA_BATCH_JOBS)
            .set(QuoteMetadataBatchJobTable.OPENAI_BATCH_ID, openAiBatchId)
            .set(QuoteMetadataBatchJobTable.INPUT_FILE_ID, inputFileId)
            .set(QuoteMetadataBatchJobTable.STATUS, status.name)
            .set(QuoteMetadataBatchJobTable.UPDATED_AT, LocalDateTime.now())
            .where(QuoteMetadataBatchJobTable.ID.eq(jobId))
            .execute()
    }

    fun updateQuoteMetadataBatchJobAsFailed(jobId: Long) {
        val now = LocalDateTime.now()

        dsl
            .update(QuoteMetadataBatchJobTable.QUOTE_METADATA_BATCH_JOBS)
            .set(QuoteMetadataBatchJobTable.STATUS, BatchJobStatus.FAILED.name)
            .set(QuoteMetadataBatchJobTable.FAILED_COUNT, QuoteMetadataBatchJobTable.SUBMITTED_COUNT)
            .set(QuoteMetadataBatchJobTable.UPDATED_AT, now)
            .set(QuoteMetadataBatchJobTable.COMPLETED_AT, now)
            .where(QuoteMetadataBatchJobTable.ID.eq(jobId))
            .execute()
    }

    fun updateQuoteMetadataBatchJobStatus(
        jobId: Long,
        status: BatchJobStatus,
        outputFileId: String?,
        errorFileId: String?,
    ) {
        val now = LocalDateTime.now()

        dsl
            .update(QuoteMetadataBatchJobTable.QUOTE_METADATA_BATCH_JOBS)
            .set(QuoteMetadataBatchJobTable.STATUS, status.name)
            .set(QuoteMetadataBatchJobTable.OUTPUT_FILE_ID, outputFileId)
            .set(QuoteMetadataBatchJobTable.ERROR_FILE_ID, errorFileId)
            .set(QuoteMetadataBatchJobTable.UPDATED_AT, now)
            .apply {
                if (status.isTerminal()) {
                    set(
                        QuoteMetadataBatchJobTable.COMPLETED_AT,
                        DSL.coalesce(QuoteMetadataBatchJobTable.COMPLETED_AT, now),
                    )
                }
                if (status.isFailedTerminal()) {
                    set(QuoteMetadataBatchJobTable.FAILED_COUNT, QuoteMetadataBatchJobTable.SUBMITTED_COUNT)
                }
            }.where(QuoteMetadataBatchJobTable.ID.eq(jobId))
            .execute()
    }

    fun updateQuoteMetadataBatchJobCounts(
        jobId: Long,
        succeededCount: Int,
        failedCount: Int,
    ) {
        dsl
            .update(QuoteMetadataBatchJobTable.QUOTE_METADATA_BATCH_JOBS)
            .set(QuoteMetadataBatchJobTable.SUCCEEDED_COUNT, succeededCount)
            .set(QuoteMetadataBatchJobTable.FAILED_COUNT, failedCount)
            .set(QuoteMetadataBatchJobTable.UPDATED_AT, LocalDateTime.now())
            .where(QuoteMetadataBatchJobTable.ID.eq(jobId))
            .execute()
    }

    fun isRunningJQuoteMetadataJob(): Boolean =
        dsl.fetchExists(
            QuoteMetadataBatchJobTable.QUOTE_METADATA_BATCH_JOBS,
            QuoteMetadataBatchJobTable.STATUS.`in`(
                BatchJobStatus.runningStatuses().map { status -> status.name },
            ),
        )

    fun countRunningJobs(): Int =
        dsl
            .selectCount()
            .from(QuoteMetadataBatchJobTable.QUOTE_METADATA_BATCH_JOBS)
            .where(
                QuoteMetadataBatchJobTable.STATUS.`in`(
                    BatchJobStatus.runningStatuses().map { status -> status.name },
                ),
            ).fetchOne(0, Int::class.java) ?: 0

    fun findActiveJob(): QuoteMetadataBatchJob? =
        dsl
            .select(QUOTE_METADATA_BATCH_JOBS_FIELDS)
            .from(QuoteMetadataBatchJobTable.QUOTE_METADATA_BATCH_JOBS)
            .where(
                QuoteMetadataBatchJobTable.STATUS.`in`(
                    BatchJobStatus.runningStatuses().map { status -> status.name },
                ),
            ).orderBy(QuoteMetadataBatchJobTable.CREATED_AT.desc())
            .limit(1)
            .fetchOne(::toQuoteMetadataBatchJob)

    fun findById(jobId: Long): QuoteMetadataBatchJob? =
        dsl
            .select(QUOTE_METADATA_BATCH_JOBS_FIELDS)
            .from(QuoteMetadataBatchJobTable.QUOTE_METADATA_BATCH_JOBS)
            .where(QuoteMetadataBatchJobTable.ID.eq(jobId))
            .fetchOne(::toQuoteMetadataBatchJob)

    private fun toQuoteMetadataBatchJob(record: Record): QuoteMetadataBatchJob =
        QuoteMetadataBatchJob(
            id = record[QuoteMetadataBatchJobTable.ID]!!,
            openAiBatchId = record[QuoteMetadataBatchJobTable.OPENAI_BATCH_ID],
            inputFileId = record[QuoteMetadataBatchJobTable.INPUT_FILE_ID],
            outputFileId = record[QuoteMetadataBatchJobTable.OUTPUT_FILE_ID],
            errorFileId = record[QuoteMetadataBatchJobTable.ERROR_FILE_ID],
            status = BatchJobStatus.from(record[QuoteMetadataBatchJobTable.STATUS]!!),
            metadataModel = record[QuoteMetadataBatchJobTable.METADATA_MODEL]!!,
            metadataVersion = record[QuoteMetadataBatchJobTable.METADATA_VERSION]!!,
            submittedCount = record[QuoteMetadataBatchJobTable.SUBMITTED_COUNT]!!,
            succeededCount = record[QuoteMetadataBatchJobTable.SUCCEEDED_COUNT]!!,
            failedCount = record[QuoteMetadataBatchJobTable.FAILED_COUNT]!!,
            createdAt = record[QuoteMetadataBatchJobTable.CREATED_AT]!!,
            updatedAt = record[QuoteMetadataBatchJobTable.UPDATED_AT]!!,
            completedAt = record[QuoteMetadataBatchJobTable.COMPLETED_AT],
        )

    private companion object {
        val QUOTE_METADATA_BATCH_JOBS_FIELDS: List<Field<*>> =
            listOf(
                QuoteMetadataBatchJobTable.ID,
                QuoteMetadataBatchJobTable.OPENAI_BATCH_ID,
                QuoteMetadataBatchJobTable.INPUT_FILE_ID,
                QuoteMetadataBatchJobTable.OUTPUT_FILE_ID,
                QuoteMetadataBatchJobTable.ERROR_FILE_ID,
                QuoteMetadataBatchJobTable.STATUS,
                QuoteMetadataBatchJobTable.METADATA_MODEL,
                QuoteMetadataBatchJobTable.METADATA_VERSION,
                QuoteMetadataBatchJobTable.SUBMITTED_COUNT,
                QuoteMetadataBatchJobTable.SUCCEEDED_COUNT,
                QuoteMetadataBatchJobTable.FAILED_COUNT,
                QuoteMetadataBatchJobTable.CREATED_AT,
                QuoteMetadataBatchJobTable.UPDATED_AT,
                QuoteMetadataBatchJobTable.COMPLETED_AT,
            )
    }
}
