package com.firstpenguin.app.domain.quotecreation.service

import com.firstpenguin.app.domain.book.model.Book
import com.firstpenguin.app.domain.book.repository.BookQuoteExtractionTargetRepository
import com.firstpenguin.app.domain.openai.dto.OpenAiBatchResponse
import com.firstpenguin.app.domain.openai.dto.OpenAiFileResponse
import com.firstpenguin.app.domain.quotebatch.model.QuoteBatchModelVersion
import com.firstpenguin.app.domain.quotebatch.model.QuoteBatchType
import com.firstpenguin.app.domain.quotebatch.repository.QuoteBatchItemRepository
import com.firstpenguin.app.domain.quotebatch.repository.QuoteBatchJobRepository
import com.firstpenguin.app.domain.quotecreation.review.repository.QuoteCandidateRepository
import com.firstpenguin.app.global.enums.BatchItemStatus
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service

@Service
class QuoteCreationBatchService(
    private val bookQuoteExtractionTargetRepository: BookQuoteExtractionTargetRepository,
    private val candidateRepository: QuoteCandidateRepository,
    private val jobRepository: QuoteBatchJobRepository,
    private val itemRepository: QuoteBatchItemRepository,
) {
    fun getBooksNeedingQuotes(limit: Int): List<Book> =
        bookQuoteExtractionTargetRepository.findBooksNeedingQuotes(
            limit = limit,
            extractionModel = QuoteBatchModelVersion.QUOTE_EXTRACTION_V1.model,
            extractionVersion = QuoteBatchModelVersion.QUOTE_EXTRACTION_V1.version,
        )

    fun getPendingTargets(limit: Int) = candidateRepository.findPendingTargets(limit = limit)

    fun createPreparingQuoteBatchJob(
        batchType: QuoteBatchType,
        submittedCount: Int,
        modelVersion: QuoteBatchModelVersion,
    ): Long =
        jobRepository.insertPreparingQuoteBatchJob(
            jobType = batchType,
            model = modelVersion.model,
            version = modelVersion.version,
            submittedCount = submittedCount,
        )

    fun createQuoteBatchItems(
        jobId: Long,
        batchType: QuoteBatchType,
        targetIds: List<Long>,
        customIdPrefix: String,
        status: BatchItemStatus,
    ) = itemRepository.insertQuoteBatchItems(jobId, batchType, targetIds, customIdPrefix, status)

    fun markQuoteBatchSubmitted(
        jobId: Long,
        batch: OpenAiBatchResponse,
        inputFile: OpenAiFileResponse,
    ) {
        jobRepository.updateQuoteBatchJobAsSubmitted(
            jobId = jobId,
            openAiBatchId = batch.id,
            inputFileId = inputFile.id,
            status = batch.status,
        )

        itemRepository.updateQuoteBatchItemsStatus(
            jobId = jobId,
            status = BatchItemStatus.SUBMITTED,
        )
    }

    fun markQuoteBatchFailed(
        jobId: Long,
        errorMessage: String?,
    ) {
        jobRepository.updateQuoteBatchJobAsFailed(jobId = jobId)
        itemRepository.updateQuoteBatchItemsStatus(
            jobId = jobId,
            status = BatchItemStatus.FAILED,
            errorMessage = errorMessage,
        )
    }

    fun validateNoRunningJob() {
        if (jobRepository.isRunningQuoteBatchJob(QUOTE_CONTENT_JOB_TYPES)) {
            throw CustomException(ErrorCode.QUOTE_CREATION_BATCH_JOB_IS_RUNNING)
        }
    }

    private companion object {
        val QUOTE_CONTENT_JOB_TYPES = listOf(QuoteBatchType.QUOTE_EXTRACTION, QuoteBatchType.QUOTE_REVIEW)
    }
}
