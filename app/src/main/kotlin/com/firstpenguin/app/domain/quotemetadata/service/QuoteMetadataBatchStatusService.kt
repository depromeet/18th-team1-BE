package com.firstpenguin.app.domain.quotemetadata.service

import com.firstpenguin.app.domain.openai.dto.OpenAiBatchStatusResponse
import com.firstpenguin.app.domain.quotebatch.model.QuoteBatchJob
import com.firstpenguin.app.domain.quotebatch.model.QuoteBatchType
import com.firstpenguin.app.domain.quotebatch.repository.QuoteBatchJobRepository
import com.firstpenguin.app.domain.quotemetadata.dto.ActiveJobStatusResponse
import com.firstpenguin.app.domain.quotemetadata.dto.QuoteMetadataBatchStatusResponse
import com.firstpenguin.app.domain.quotemetadata.repository.QuoteMetadataBatchItemRepository
import com.firstpenguin.app.domain.quotemetadata.repository.QuoteMetadataRepository
import com.firstpenguin.app.global.enums.BatchItemStatus
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service

@Service
class QuoteMetadataBatchStatusService(
    private val quoteMetadataRepository: QuoteMetadataRepository,
    private val quoteBatchJobRepository: QuoteBatchJobRepository,
    private val quoteMetadataBatchItemRepository: QuoteMetadataBatchItemRepository,
) {
    fun getTotalStatus(): QuoteMetadataBatchStatusResponse =
        createStatusResponse(quoteBatchJobRepository.findActiveJob(QUOTE_METADATA_JOB_TYPES))

    fun getStatus(jobId: Long): QuoteMetadataBatchStatusResponse =
        createStatusResponse(
            getJob(jobId) ?: throw CustomException(ErrorCode.QUOTE_METADATA_BATCH_JOB_NOT_FOUND),
        )

    private fun createStatusResponse(job: QuoteBatchJob?): QuoteMetadataBatchStatusResponse =
        QuoteMetadataBatchStatusResponse(
            totalQuoteCount = quoteMetadataRepository.countTotalQuotes(),
            createdCount = quoteMetadataRepository.countCreatedMetadata(),
            pendingCount = quoteMetadataRepository.countPendingQuotes(),
            processingCount = quoteMetadataBatchItemRepository.countActiveItemsWithoutMetadata(),
            failedCount = quoteMetadataBatchItemRepository.countFailedItemsWithoutMetadata(),
            runningJobCount = quoteBatchJobRepository.countRunningJobs(QUOTE_METADATA_JOB_TYPES),
            activeJob = job?.toResponse(),
        )

    fun getActiveJob(): QuoteBatchJob? = quoteBatchJobRepository.findActiveJob(QUOTE_METADATA_JOB_TYPES)

    fun getJob(jobId: Long): QuoteBatchJob? = findMetadataJob(jobId)

    fun updateQuoteMetadataBatchJobStatus(
        jobId: Long,
        batch: OpenAiBatchStatusResponse,
    ) {
        quoteBatchJobRepository.updateQuoteBatchJobStatus(
            jobId = jobId,
            status = batch.status,
            outputFileId = batch.outputFileId,
            errorFileId = batch.errorFileId,
        )

        if (batch.status.isFailedTerminal()) {
            quoteMetadataBatchItemRepository.updateQuoteMetadataBatchItemsStatus(
                jobId = jobId,
                status = BatchItemStatus.FAILED,
                errorMessage = batch.failedMessage(),
            )
        }
    }

    private fun QuoteBatchJob.toResponse(): ActiveJobStatusResponse =
        ActiveJobStatusResponse(
            jobId = id,
            openAiBatchId = openAiBatchId,
            submittedCount = submittedCount,
            status = status,
        )

    private fun findMetadataJob(jobId: Long): QuoteBatchJob? {
        val jobTypes = QUOTE_METADATA_JOB_TYPES
        return quoteBatchJobRepository.findByIdAndJobType(jobId, jobTypes)
    }

    private companion object {
        val QUOTE_METADATA_JOB_TYPES = listOf(QuoteBatchType.QUOTE_METADATA)
    }
}

private fun OpenAiBatchStatusResponse.failedMessage(): String = errorMessage ?: "OpenAI batch status: ${status.name}"
