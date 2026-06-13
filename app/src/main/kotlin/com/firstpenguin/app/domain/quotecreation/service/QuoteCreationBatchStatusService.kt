package com.firstpenguin.app.domain.quotecreation.service

import com.firstpenguin.app.domain.book.repository.BookQuoteExtractionTargetRepository
import com.firstpenguin.app.domain.book.repository.BookRepository
import com.firstpenguin.app.domain.openai.dto.OpenAiBatchStatusResponse
import com.firstpenguin.app.domain.quotebatch.model.QuoteBatchJob
import com.firstpenguin.app.domain.quotebatch.model.QuoteBatchModelVersion
import com.firstpenguin.app.domain.quotebatch.model.QuoteBatchType
import com.firstpenguin.app.domain.quotebatch.repository.QuoteBatchItemRepository
import com.firstpenguin.app.domain.quotebatch.repository.QuoteBatchJobRepository
import com.firstpenguin.app.domain.quotecreation.dto.QuoteCreationBatchActiveJobStatusResponse
import com.firstpenguin.app.domain.quotecreation.dto.QuoteCreationBatchStatusResponse
import com.firstpenguin.app.global.enums.BatchItemStatus
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service

@Service
class QuoteCreationBatchStatusService(
    private val bookRepository: BookRepository,
    private val bookQuoteExtractionTargetRepository: BookQuoteExtractionTargetRepository,
    private val jobRepository: QuoteBatchJobRepository,
    private val itemRepository: QuoteBatchItemRepository,
) {
    fun getTotalStatus(): QuoteCreationBatchStatusResponse = createStatusResponse(findLatestActiveJob())

    fun getStatus(jobId: Long): QuoteCreationBatchStatusResponse =
        createStatusResponse(
            getJob(jobId) ?: throw CustomException(ErrorCode.QUOTE_CREATION_BATCH_JOB_NOT_FOUND),
        )

    private fun createStatusResponse(job: QuoteBatchJob?): QuoteCreationBatchStatusResponse =
        QuoteCreationBatchStatusResponse(
            totalBookCount = bookRepository.countActiveBooks(),
            extractedBookCount = bookRepository.countBooksWithRecommendedQuotes(),
            pendingBookCount = countBooksNeedingCurrentQuoteExtraction(),
            processingBookCount = itemRepository.countItems(QUOTE_CONTENT_JOB_TYPES, BatchItemStatus.activeStatuses()),
            failedBookCount = itemRepository.countItems(QUOTE_CONTENT_JOB_TYPES, listOf(BatchItemStatus.FAILED)),
            runningJobCount = jobRepository.countRunningJobs(QUOTE_CONTENT_JOB_TYPES),
            activeJob = job?.toResponse(),
        )

    fun getActiveJob(): QuoteBatchJob? = findLatestActiveJob()

    private fun findLatestActiveJob(): QuoteBatchJob? = jobRepository.findActiveJob(QUOTE_CONTENT_JOB_TYPES)

    private fun countBooksNeedingCurrentQuoteExtraction(): Int =
        bookQuoteExtractionTargetRepository.countBooksNeedingQuotes(
            extractionModel = QuoteBatchModelVersion.QUOTE_EXTRACTION_V1.model,
            extractionVersion = QuoteBatchModelVersion.QUOTE_EXTRACTION_V1.version,
        )

    fun getJob(jobId: Long): QuoteBatchJob? = jobRepository.findByIdAndJobType(jobId, QUOTE_CONTENT_JOB_TYPES)

    fun updateQuoteBatchJobStatus(
        jobId: Long,
        batch: OpenAiBatchStatusResponse,
    ) {
        jobRepository.updateQuoteBatchJobStatus(
            jobId = jobId,
            status = batch.status,
            outputFileId = batch.outputFileId,
            errorFileId = batch.errorFileId,
        )

        if (batch.status.isFailedTerminal()) {
            itemRepository.updateQuoteBatchItemsStatus(
                jobId = jobId,
                status = BatchItemStatus.FAILED,
                errorMessage = "OpenAI batch status: ${batch.status.name}",
            )
        }
    }

    private fun QuoteBatchJob.toResponse(): QuoteCreationBatchActiveJobStatusResponse =
        QuoteCreationBatchActiveJobStatusResponse(
            jobId = id,
            openAiBatchId = openAiBatchId,
            submittedCount = submittedCount,
            status = status,
        )

    private companion object {
        val QUOTE_CONTENT_JOB_TYPES = listOf(QuoteBatchType.QUOTE_EXTRACTION, QuoteBatchType.QUOTE_REVIEW)
    }
}
