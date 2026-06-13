package com.firstpenguin.app.domain.quotecreation.usecase

import com.firstpenguin.app.domain.book.model.Book
import com.firstpenguin.app.domain.openai.dto.OpenAiBatchResponse
import com.firstpenguin.app.domain.openai.dto.OpenAiBatchStatusResponse
import com.firstpenguin.app.domain.openai.dto.OpenAiFileResponse
import com.firstpenguin.app.domain.quotebatch.model.QuoteBatchModelVersion
import com.firstpenguin.app.domain.quotebatch.model.QuoteBatchType
import com.firstpenguin.app.domain.quotecreation.dto.ParsedQuoteCreationBatchResult
import com.firstpenguin.app.domain.quotecreation.review.model.QuoteReviewBatchTarget
import com.firstpenguin.app.domain.quotecreation.service.QuoteCreationBatchResultService
import com.firstpenguin.app.domain.quotecreation.service.QuoteCreationBatchService
import com.firstpenguin.app.domain.quotecreation.service.QuoteCreationBatchStatusService
import com.firstpenguin.app.global.enums.BatchItemStatus
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class QuoteCreationBatchCommandUseCase(
    private val batchService: QuoteCreationBatchService,
    private val statusService: QuoteCreationBatchStatusService,
    private val resultService: QuoteCreationBatchResultService,
) {
    @Transactional
    fun prepareExtractionBatch(limit: Int): PreparedQuoteExtractionBatch {
        val books = batchService.getBooksNeedingQuotes(limit = limit)
        if (books.isEmpty()) throw CustomException(ErrorCode.QUOTE_EXTRACTION_BATCH_TARGET_NOT_FOUND)

        val jobId =
            batchService.createPreparingQuoteBatchJob(
                batchType = QuoteBatchType.QUOTE_EXTRACTION,
                submittedCount = INITIAL_SUBMITTED_COUNT,
                modelVersion = QuoteBatchModelVersion.QUOTE_EXTRACTION_V1,
            )
        val claimedBookIds =
            batchService
                .createQuoteBatchItems(
                    jobId = jobId,
                    batchType = QuoteBatchType.QUOTE_EXTRACTION,
                    targetIds = books.map { book -> book.id },
                    customIdPrefix = BOOK_CUSTOM_ID_PREFIX,
                    status = BatchItemStatus.PREPARING,
                ).toSet()
        val claimedBooks = books.filter { book -> book.id in claimedBookIds }
        if (claimedBooks.isEmpty()) throw CustomException(ErrorCode.QUOTE_EXTRACTION_BATCH_TARGET_NOT_FOUND)

        batchService.updateSubmittedCount(jobId, claimedBooks.size)
        return PreparedQuoteExtractionBatch(jobId, claimedBooks)
    }

    @Transactional
    fun prepareCandidateReviewBatch(limit: Int): PreparedCandidateReviewBatch {
        val targets = batchService.getPendingTargets(limit = limit)
        if (targets.isEmpty()) throw CustomException(ErrorCode.QUOTE_REVIEW_BATCH_TARGET_NOT_FOUND)
        return createPreparedReviewBatch(targets)
    }

    private fun createPreparedReviewBatch(targets: List<QuoteReviewBatchTarget>): PreparedCandidateReviewBatch {
        val jobId =
            batchService.createPreparingQuoteBatchJob(
                batchType = QuoteBatchType.QUOTE_REVIEW,
                submittedCount = INITIAL_SUBMITTED_COUNT,
                modelVersion = QuoteBatchModelVersion.QUOTE_REVIEW_V1,
            )
        val claimedBookIds = createReviewBatchItems(jobId, targets)
        val claimedTargets = targets.filter { target -> target.book.id in claimedBookIds }
        if (claimedTargets.isEmpty()) throw CustomException(ErrorCode.QUOTE_REVIEW_BATCH_TARGET_NOT_FOUND)

        batchService.updateSubmittedCount(jobId, claimedTargets.size)
        return PreparedCandidateReviewBatch(jobId, claimedTargets)
    }

    private fun createReviewBatchItems(
        jobId: Long,
        targets: List<QuoteReviewBatchTarget>,
    ): Set<Long> =
        batchService
            .createQuoteBatchItems(
                jobId = jobId,
                batchType = QuoteBatchType.QUOTE_REVIEW,
                targetIds = targets.map { target -> target.book.id },
                customIdPrefix = BOOK_CUSTOM_ID_PREFIX,
                status = BatchItemStatus.PREPARING,
            ).toSet()

    @Transactional
    fun markBatchSubmitted(
        jobId: Long,
        batch: OpenAiBatchResponse,
        inputFile: OpenAiFileResponse,
    ) = batchService.markQuoteBatchSubmitted(jobId, batch, inputFile)

    @Transactional
    fun markBatchFailed(
        jobId: Long,
        errorMessage: String?,
    ) = batchService.markQuoteBatchFailed(jobId, errorMessage)

    @Transactional
    fun syncBatchStatus(
        jobId: Long,
        batch: OpenAiBatchStatusResponse,
    ) = statusService.updateQuoteBatchJobStatus(jobId, batch)

    @Transactional
    fun saveBatchResults(
        jobId: Long,
        results: List<ParsedQuoteCreationBatchResult>,
    ) = resultService.saveBatchResults(jobId, results)
}

data class PreparedQuoteExtractionBatch(
    val jobId: Long,
    val books: List<Book>,
)

data class PreparedCandidateReviewBatch(
    val jobId: Long,
    val targets: List<QuoteReviewBatchTarget>,
)

private const val BOOK_CUSTOM_ID_PREFIX = "book"
private const val INITIAL_SUBMITTED_COUNT = 0
