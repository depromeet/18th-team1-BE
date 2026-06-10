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
    private val quoteCandidateRepository: QuoteCandidateRepository,
    private val quoteBatchJobRepository: QuoteBatchJobRepository,
    private val quoteBatchItemRepository: QuoteBatchItemRepository,
) {
    fun getBooksNeedingQuotes(limit: Int): List<Book> =
        bookQuoteExtractionTargetRepository.findBooksNeedingQuotes(
            limit = limit,
            extractionModel = QuoteBatchModelVersion.QUOTE_EXTRACTION_V1.model,
            extractionVersion = QuoteBatchModelVersion.QUOTE_EXTRACTION_V1.version,
        )

    fun getPendingTargets(limit: Int) = quoteCandidateRepository.findPendingTargets(limit = limit)

    fun createPreparingQuoteBatchJob(
        batchType: QuoteBatchType,
        submittedCount: Int,
        modelVersion: QuoteBatchModelVersion,
    ): Long =
        quoteBatchJobRepository.insertPreparingQuoteBatchJob(
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
    ) = quoteBatchItemRepository.insertQuoteBatchItems(jobId, batchType, targetIds, customIdPrefix, status)

    fun markQuoteBatchSubmitted(
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

    fun markQuoteBatchFailed(
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
        private val RUNNING_BLOCKED_JOB_TYPES = QuoteBatchType.entries.toList()
    }
}
