package com.firstpenguin.app.domain.quotemetadata.service

import com.firstpenguin.app.domain.emotion.repository.TagRepository
import com.firstpenguin.app.domain.openai.dto.OpenAiBatchResponse
import com.firstpenguin.app.domain.openai.dto.OpenAiFileResponse
import com.firstpenguin.app.domain.quote.model.Quote
import com.firstpenguin.app.domain.quotebatch.model.QuoteBatchModelVersion
import com.firstpenguin.app.domain.quotebatch.model.QuoteBatchType
import com.firstpenguin.app.domain.quotebatch.repository.QuoteBatchItemRepository
import com.firstpenguin.app.domain.quotebatch.repository.QuoteBatchJobRepository
import com.firstpenguin.app.domain.quotemetadata.dto.TagOption
import com.firstpenguin.app.domain.quotemetadata.repository.QuoteMetadataRepository
import com.firstpenguin.app.global.enums.BatchItemStatus
import com.firstpenguin.app.global.enums.TagType
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service

@Service
class QuoteMetadataBatchService(
    private val quoteMetadataRepository: QuoteMetadataRepository,
    private val quoteBatchJobRepository: QuoteBatchJobRepository,
    private val quoteBatchItemRepository: QuoteBatchItemRepository,
    private val tagRepository: TagRepository,
) {
    fun getPendingQuotes(limit: Int): List<Quote> = quoteMetadataRepository.findPendingQuotes(limit = limit)

    fun getAllTagsByType(): Map<TagType, List<TagOption>> = tagRepository.getActiveTagsByType()

    fun createPreparingQuoteMetadataBatchJob(submittedCount: Int): Long =
        quoteBatchJobRepository.insertPreparingQuoteBatchJob(
            jobType = QuoteBatchType.QUOTE_METADATA,
            model = QuoteBatchModelVersion.QUOTE_METADATA_V1.model,
            version = QuoteBatchModelVersion.QUOTE_METADATA_V1.version,
            submittedCount = submittedCount,
        )

    fun createQuoteMetadataBatchItem(
        jobId: Long,
        quoteIds: List<Long>,
        status: BatchItemStatus,
    ) = quoteBatchItemRepository.insertQuoteBatchItems(
        jobId = jobId,
        jobType = QuoteBatchType.QUOTE_METADATA,
        targetIds = quoteIds,
        customIdPrefix = QUOTE_CUSTOM_ID_PREFIX,
        status = status,
    )

    fun markQuoteMetadataBatchSubmitted(
        jobId: Long,
        batch: OpenAiBatchResponse,
        inputFile: OpenAiFileResponse,
    ) {
        quoteBatchJobRepository.updateQuoteBatchJobAsSubmitted(
            jobId = jobId,
            openAiBatchId = batch.id,
            inputFileId = inputFile.id,
            status = batch.status,
        )

        quoteBatchItemRepository.updateQuoteBatchItemsStatus(
            jobId = jobId,
            status = BatchItemStatus.SUBMITTED,
        )
    }

    fun markQuoteMetadataBatchFailed(
        jobId: Long,
        errorMessage: String?,
    ) {
        quoteBatchJobRepository.updateQuoteBatchJobAsFailed(jobId = jobId)
        quoteBatchItemRepository.updateQuoteBatchItemsStatus(
            jobId = jobId,
            status = BatchItemStatus.FAILED,
            errorMessage = errorMessage,
        )
    }

    fun validateNoRunningJob() {
        if (quoteBatchJobRepository.isRunningQuoteBatchJob(RUNNING_BLOCKED_JOB_TYPES)) {
            throw CustomException(ErrorCode.QUOTE_BATCH_JOB_IS_RUNNING)
        }
    }

    private companion object {
        const val QUOTE_CUSTOM_ID_PREFIX = "quote"
        private val RUNNING_BLOCKED_JOB_TYPES = QuoteBatchType.entries.toList()
    }
}
