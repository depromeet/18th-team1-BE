package com.firstpenguin.app.domain.batch.service

import com.firstpenguin.app.domain.batch.dto.ActiveJobStatusResponse
import com.firstpenguin.app.domain.batch.dto.ParsedBatchQuoteResult
import com.firstpenguin.app.domain.batch.dto.QuoteMetadataBatchStatusResponse
import com.firstpenguin.app.domain.batch.dto.TagOption
import com.firstpenguin.app.domain.batch.dto.ai.OpenAiBatchResponse
import com.firstpenguin.app.domain.batch.dto.ai.OpenAiBatchStatusResponse
import com.firstpenguin.app.domain.batch.dto.ai.OpenAiFileResponse
import com.firstpenguin.app.domain.batch.model.QuoteMetadataBatchJob
import com.firstpenguin.app.domain.batch.repository.QuoteMetadataBatchItemRepository
import com.firstpenguin.app.domain.batch.repository.QuoteMetadataBatchJobRepository
import com.firstpenguin.app.domain.batch.repository.QuoteMetadataRepository
import com.firstpenguin.app.domain.emotion.repository.TagRepository
import com.firstpenguin.app.domain.quote.model.Quote
import com.firstpenguin.app.global.enums.BatchItemStatus
import com.firstpenguin.app.global.enums.QuoteMetadataBatchModelVersion
import com.firstpenguin.app.global.enums.TagType
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service

@Service
class QuoteMetadataService(
    private val quoteMetadataRepository: QuoteMetadataRepository,
    private val quoteMetadataBatchJobRepository: QuoteMetadataBatchJobRepository,
    private val quoteMetadataBatchItemRepository: QuoteMetadataBatchItemRepository,
    private val tagRepository: TagRepository,
) {
    fun getPendingQuotes(limit: Int): List<Quote> = quoteMetadataRepository.findPendingQuotes(limit = limit)

    fun getAllTagsByType(): Map<TagType, List<TagOption>> = tagRepository.getActiveTagsByType()

    fun createPreparingQuoteMetadataBatchJob(submittedCount: Int): Long =
        quoteMetadataBatchJobRepository.insertPreparingQuoteMetadataBatchJob(
            metadataModel = QuoteMetadataBatchModelVersion.V1.model,
            metadataVersion = QuoteMetadataBatchModelVersion.V1.version,
            submittedCount = submittedCount,
        )

    fun createQuoteMetadataBatchItem(
        jobId: Long,
        quoteIds: List<Long>,
        status: BatchItemStatus,
    ) = quoteMetadataBatchItemRepository.insertQuoteMetadataBatchItems(
        jobId = jobId,
        quoteIds = quoteIds,
        status = status,
    )

    fun markQuoteMetadataBatchSubmitted(
        jobId: Long,
        batch: OpenAiBatchResponse,
        inputFile: OpenAiFileResponse,
    ) {
        quoteMetadataBatchJobRepository.updateQuoteMetadataBatchJobAsSubmitted(
            jobId = jobId,
            openAiBatchId = batch.id,
            inputFileId = inputFile.id,
            status = batch.status,
        )

        quoteMetadataBatchItemRepository.updateQuoteMetadataBatchItemsStatus(
            jobId = jobId,
            status = BatchItemStatus.SUBMITTED,
        )
    }

    fun markQuoteMetadataBatchFailed(
        jobId: Long,
        errorMessage: String?,
    ) {
        quoteMetadataBatchJobRepository.updateQuoteMetadataBatchJobAsFailed(jobId = jobId)
        quoteMetadataBatchItemRepository.updateQuoteMetadataBatchItemsStatus(
            jobId = jobId,
            status = BatchItemStatus.FAILED,
            errorMessage = errorMessage,
        )
    }

    fun getStatus(): QuoteMetadataBatchStatusResponse =
        QuoteMetadataBatchStatusResponse(
            totalQuoteCount = quoteMetadataRepository.countTotalQuotes(),
            createdCount = quoteMetadataRepository.countCreatedMetadata(),
            pendingCount = quoteMetadataRepository.countPendingQuotes(),
            processingCount = quoteMetadataBatchItemRepository.countActiveItemsWithoutMetadata(),
            failedCount = quoteMetadataBatchItemRepository.countFailedItemsWithoutMetadata(),
            runningJobCount = quoteMetadataBatchJobRepository.countRunningJobs(),
            activeJob = quoteMetadataBatchJobRepository.findActiveJob()?.toResponse(),
        )

    fun getActiveJob(): QuoteMetadataBatchJob? = quoteMetadataBatchJobRepository.findActiveJob()

    fun getJob(jobId: Long): QuoteMetadataBatchJob? = quoteMetadataBatchJobRepository.findById(jobId)

    fun updateQuoteMetadataBatchJobStatus(
        jobId: Long,
        batch: OpenAiBatchStatusResponse,
    ) {
        val status = batch.status

        quoteMetadataBatchJobRepository.updateQuoteMetadataBatchJobStatus(
            jobId = jobId,
            status = status,
            outputFileId = batch.outputFileId,
            errorFileId = batch.errorFileId,
        )

        if (status.isFailedTerminal()) {
            quoteMetadataBatchItemRepository.updateQuoteMetadataBatchItemsStatus(
                jobId = jobId,
                status = BatchItemStatus.FAILED,
                errorMessage = "OpenAI batch status: ${status.name}",
            )
        }
    }

    fun saveBatchResults(
        jobId: Long,
        results: List<ParsedBatchQuoteResult>,
    ) {
        val tagIdByCode = getTagIdByCode()
        val statuses = results.map { result -> saveBatchResult(jobId, result, tagIdByCode) }

        quoteMetadataBatchJobRepository.updateQuoteMetadataBatchJobCounts(
            jobId = jobId,
            succeededCount = statuses.count { status -> status == BatchItemStatus.SUCCEEDED },
            failedCount = statuses.count { status -> status == BatchItemStatus.FAILED },
        )
    }

    fun validateNoRunningJob() {
        val isRunningJob = quoteMetadataBatchJobRepository.isRunningJQuoteMetadataJob()

        if (isRunningJob) {
            throw CustomException(ErrorCode.QUOTE_METADATA_BATCH_JOB_IS_RUNNING)
        }
    }

    private fun saveBatchResult(
        jobId: Long,
        result: ParsedBatchQuoteResult,
        tagIdByCode: Map<String, Long>,
    ): BatchItemStatus {
        val quoteId = result.quoteId ?: return BatchItemStatus.FAILED

        if (result.errorMessage != null) {
            markBatchItemFailed(jobId, quoteId, result.errorMessage)
            return BatchItemStatus.FAILED
        }

        return runCatching {
            saveSucceededBatchResult(jobId, quoteId, result, tagIdByCode)
        }.getOrElse { exception ->
            markBatchItemFailed(jobId, quoteId, exception.message)
            BatchItemStatus.FAILED
        }
    }

    private fun saveSucceededBatchResult(
        jobId: Long,
        quoteId: Long,
        result: ParsedBatchQuoteResult,
        tagIdByCode: Map<String, Long>,
    ): BatchItemStatus {
        val metadata = result.toQuoteMetadata(QuoteMetadataBatchModelVersion.V1)
        val metadataId = quoteMetadataRepository.upsertQuoteMetadata(metadata)
        val tags = result.toQuoteMetadataTags(metadataId, tagIdByCode)

        quoteMetadataRepository.replaceQuoteMetadataTags(metadataId, tags)
        markBatchItemSucceeded(jobId, quoteId)

        return BatchItemStatus.SUCCEEDED
    }

    private fun markBatchItemSucceeded(
        jobId: Long,
        quoteId: Long,
    ) = quoteMetadataBatchItemRepository.updateQuoteMetadataBatchItemStatus(
        jobId = jobId,
        quoteId = quoteId,
        status = BatchItemStatus.SUCCEEDED,
    )

    private fun markBatchItemFailed(
        jobId: Long,
        quoteId: Long,
        errorMessage: String?,
    ) = quoteMetadataBatchItemRepository.updateQuoteMetadataBatchItemStatus(
        jobId = jobId,
        quoteId = quoteId,
        status = BatchItemStatus.FAILED,
        errorMessage = errorMessage,
    )

    private fun getTagIdByCode(): Map<String, Long> =
        tagRepository
            .getActiveTagsByType()
            .values
            .flatten()
            .associate { tag -> tag.code to tag.id }

    private fun QuoteMetadataBatchJob.toResponse(): ActiveJobStatusResponse =
        ActiveJobStatusResponse(
            jobId = id,
            openAiBatchId = openAiBatchId,
            submittedCount = submittedCount,
            status = status,
        )
}
