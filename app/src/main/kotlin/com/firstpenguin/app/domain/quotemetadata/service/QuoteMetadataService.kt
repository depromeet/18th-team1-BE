package com.firstpenguin.app.domain.quotemetadata.service

import com.firstpenguin.app.domain.emotion.repository.TagRepository
import com.firstpenguin.app.domain.openai.dto.OpenAiBatchResponse
import com.firstpenguin.app.domain.openai.dto.OpenAiFileResponse
import com.firstpenguin.app.domain.quote.model.Quote
import com.firstpenguin.app.domain.quotemetadata.dto.TagOption
import com.firstpenguin.app.domain.quotemetadata.model.QuoteMetadataBatchModelVersion
import com.firstpenguin.app.domain.quotemetadata.repository.QuoteMetadataBatchItemRepository
import com.firstpenguin.app.domain.quotemetadata.repository.QuoteMetadataBatchJobRepository
import com.firstpenguin.app.domain.quotemetadata.repository.QuoteMetadataRepository
import com.firstpenguin.app.global.enums.BatchItemStatus
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

    fun validateNoRunningJob() {
        val isRunningJob = quoteMetadataBatchJobRepository.isRunningJQuoteMetadataJob()

        if (isRunningJob) {
            throw CustomException(ErrorCode.QUOTE_METADATA_BATCH_JOB_IS_RUNNING)
        }
    }
}
