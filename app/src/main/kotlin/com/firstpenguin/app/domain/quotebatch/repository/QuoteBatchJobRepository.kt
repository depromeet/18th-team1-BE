package com.firstpenguin.app.domain.quotebatch.repository

import com.firstpenguin.app.domain.quotebatch.model.QuoteBatchJob
import com.firstpenguin.app.domain.quotebatch.model.QuoteBatchType
import com.firstpenguin.app.domain.quotebatch.repository.table.QuoteBatchJobTable
import com.firstpenguin.app.global.enums.BatchJobStatus
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class QuoteBatchJobRepository(
    private val dsl: DSLContext,
) {
    fun insertPreparingQuoteBatchJob(
        jobType: QuoteBatchType,
        model: String,
        version: Int,
        submittedCount: Int,
    ): Long {
        val now = LocalDateTime.now()

        return dsl
            .insertInto(QuoteBatchJobTable.QUOTE_BATCH_JOBS)
            .set(QuoteBatchJobTable.STATUS, BatchJobStatus.PREPARING.name)
            .set(QuoteBatchJobTable.JOB_TYPE, jobType.name)
            .set(QuoteBatchJobTable.MODEL, model)
            .set(QuoteBatchJobTable.VERSION, version)
            .set(QuoteBatchJobTable.SUBMITTED_COUNT, submittedCount)
            .set(QuoteBatchJobTable.CREATED_AT, now)
            .set(QuoteBatchJobTable.UPDATED_AT, now)
            .returningResult(QuoteBatchJobTable.ID)
            .fetchOne(QuoteBatchJobTable.ID)
            ?: throw CustomException(ErrorCode.INTERNAL_SERVER_ERROR)
    }

    fun updateQuoteBatchJobAsSubmitted(
        jobId: Long,
        openAiBatchId: String,
        inputFileId: String,
        status: BatchJobStatus,
    ) {
        dsl
            .update(QuoteBatchJobTable.QUOTE_BATCH_JOBS)
            .set(QuoteBatchJobTable.OPENAI_BATCH_ID, openAiBatchId)
            .set(QuoteBatchJobTable.INPUT_FILE_ID, inputFileId)
            .set(QuoteBatchJobTable.STATUS, status.name)
            .set(QuoteBatchJobTable.UPDATED_AT, LocalDateTime.now())
            .where(QuoteBatchJobTable.ID.eq(jobId))
            .execute()
    }

    fun updateQuoteBatchJobAsFailed(jobId: Long) {
        val now = LocalDateTime.now()

        dsl
            .update(QuoteBatchJobTable.QUOTE_BATCH_JOBS)
            .set(QuoteBatchJobTable.STATUS, BatchJobStatus.FAILED.name)
            .set(QuoteBatchJobTable.FAILED_COUNT, QuoteBatchJobTable.SUBMITTED_COUNT)
            .set(QuoteBatchJobTable.UPDATED_AT, now)
            .set(QuoteBatchJobTable.COMPLETED_AT, now)
            .where(QuoteBatchJobTable.ID.eq(jobId))
            .execute()
    }

    fun updateQuoteBatchJobStatus(
        jobId: Long,
        status: BatchJobStatus,
        outputFileId: String?,
        errorFileId: String?,
    ) {
        val now = LocalDateTime.now()

        dsl
            .update(QuoteBatchJobTable.QUOTE_BATCH_JOBS)
            .set(QuoteBatchJobTable.STATUS, status.name)
            .set(QuoteBatchJobTable.OUTPUT_FILE_ID, outputFileId)
            .set(QuoteBatchJobTable.ERROR_FILE_ID, errorFileId)
            .set(QuoteBatchJobTable.UPDATED_AT, now)
            .apply {
                if (status.isTerminal()) {
                    set(
                        QuoteBatchJobTable.COMPLETED_AT,
                        DSL.coalesce(QuoteBatchJobTable.COMPLETED_AT, now),
                    )
                }
                if (status.isFailedTerminal()) {
                    set(QuoteBatchJobTable.FAILED_COUNT, QuoteBatchJobTable.SUBMITTED_COUNT)
                }
            }.where(QuoteBatchJobTable.ID.eq(jobId))
            .execute()
    }

    fun updateQuoteBatchJobCounts(
        jobId: Long,
        succeededCount: Int,
        failedCount: Int,
    ) {
        dsl
            .update(QuoteBatchJobTable.QUOTE_BATCH_JOBS)
            .set(QuoteBatchJobTable.SUCCEEDED_COUNT, succeededCount)
            .set(QuoteBatchJobTable.FAILED_COUNT, failedCount)
            .set(QuoteBatchJobTable.UPDATED_AT, LocalDateTime.now())
            .where(QuoteBatchJobTable.ID.eq(jobId))
            .execute()
    }

    fun isRunningQuoteBatchJob(jobTypes: List<QuoteBatchType>): Boolean =
        dsl.fetchExists(
            QuoteBatchJobTable.QUOTE_BATCH_JOBS,
            runningJobCondition(jobTypes),
        )

    fun countRunningJobs(jobTypes: List<QuoteBatchType>): Int =
        dsl
            .selectCount()
            .from(QuoteBatchJobTable.QUOTE_BATCH_JOBS)
            .where(runningJobCondition(jobTypes))
            .fetchOne(0, Int::class.java) ?: 0

    fun findActiveJob(jobTypes: List<QuoteBatchType>): QuoteBatchJob? =
        dsl
            .select(QUOTE_BATCH_JOB_FIELDS)
            .from(QuoteBatchJobTable.QUOTE_BATCH_JOBS)
            .where(runningJobCondition(jobTypes))
            .orderBy(QuoteBatchJobTable.CREATED_AT.desc())
            .limit(1)
            .fetchOne(::toQuoteBatchJob)

    fun findByIdAndJobType(
        jobId: Long,
        jobTypes: List<QuoteBatchType>,
    ): QuoteBatchJob? =
        dsl
            .select(QUOTE_BATCH_JOB_FIELDS)
            .from(QuoteBatchJobTable.QUOTE_BATCH_JOBS)
            .where(QuoteBatchJobTable.ID.eq(jobId))
            .and(QuoteBatchJobTable.JOB_TYPE.`in`(jobTypeNames(jobTypes)))
            .fetchOne(::toQuoteBatchJob)

    private fun toQuoteBatchJob(record: Record): QuoteBatchJob =
        QuoteBatchJob(
            id = record[QuoteBatchJobTable.ID]!!,
            openAiBatchId = record[QuoteBatchJobTable.OPENAI_BATCH_ID],
            inputFileId = record[QuoteBatchJobTable.INPUT_FILE_ID],
            outputFileId = record[QuoteBatchJobTable.OUTPUT_FILE_ID],
            errorFileId = record[QuoteBatchJobTable.ERROR_FILE_ID],
            status = BatchJobStatus.from(record[QuoteBatchJobTable.STATUS]!!),
            jobType = QuoteBatchType.valueOf(record[QuoteBatchJobTable.JOB_TYPE]!!),
            model = record[QuoteBatchJobTable.MODEL]!!,
            version = record[QuoteBatchJobTable.VERSION]!!,
            submittedCount = record[QuoteBatchJobTable.SUBMITTED_COUNT]!!,
            succeededCount = record[QuoteBatchJobTable.SUCCEEDED_COUNT]!!,
            failedCount = record[QuoteBatchJobTable.FAILED_COUNT]!!,
            createdAt = record[QuoteBatchJobTable.CREATED_AT]!!,
            updatedAt = record[QuoteBatchJobTable.UPDATED_AT]!!,
            completedAt = record[QuoteBatchJobTable.COMPLETED_AT],
        )

    private companion object {
        fun runningStatusNames(): List<String> = BatchJobStatus.runningStatuses().map { status -> status.name }

        fun jobTypeNames(jobTypes: List<QuoteBatchType>): List<String> = jobTypes.map { jobType -> jobType.name }

        fun runningJobCondition(jobTypes: List<QuoteBatchType>): Condition =
            QuoteBatchJobTable.STATUS
                .`in`(runningStatusNames())
                .and(QuoteBatchJobTable.JOB_TYPE.`in`(jobTypeNames(jobTypes)))

        val QUOTE_BATCH_JOB_FIELDS: List<Field<*>> =
            listOf(
                QuoteBatchJobTable.ID,
                QuoteBatchJobTable.JOB_TYPE,
                QuoteBatchJobTable.OPENAI_BATCH_ID,
                QuoteBatchJobTable.INPUT_FILE_ID,
                QuoteBatchJobTable.OUTPUT_FILE_ID,
                QuoteBatchJobTable.ERROR_FILE_ID,
                QuoteBatchJobTable.STATUS,
                QuoteBatchJobTable.MODEL,
                QuoteBatchJobTable.VERSION,
                QuoteBatchJobTable.SUBMITTED_COUNT,
                QuoteBatchJobTable.SUCCEEDED_COUNT,
                QuoteBatchJobTable.FAILED_COUNT,
                QuoteBatchJobTable.CREATED_AT,
                QuoteBatchJobTable.UPDATED_AT,
                QuoteBatchJobTable.COMPLETED_AT,
            )
    }
}
