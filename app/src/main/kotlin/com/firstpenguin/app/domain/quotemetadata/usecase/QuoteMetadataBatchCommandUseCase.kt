package com.firstpenguin.app.domain.quotemetadata.usecase

import com.firstpenguin.app.domain.openai.dto.OpenAiBatchResponse
import com.firstpenguin.app.domain.openai.dto.OpenAiBatchStatusResponse
import com.firstpenguin.app.domain.openai.dto.OpenAiFileResponse
import com.firstpenguin.app.domain.quote.model.Quote
import com.firstpenguin.app.domain.quotemetadata.dto.ParsedBatchQuoteResult
import com.firstpenguin.app.domain.quotemetadata.service.QuoteMetadataBatchResultService
import com.firstpenguin.app.domain.quotemetadata.service.QuoteMetadataBatchService
import com.firstpenguin.app.domain.quotemetadata.service.QuoteMetadataBatchStatusService
import com.firstpenguin.app.global.enums.BatchItemStatus
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class QuoteMetadataBatchCommandUseCase(
    private val quoteMetadataBatchService: QuoteMetadataBatchService,
    private val quoteMetadataBatchStatusService: QuoteMetadataBatchStatusService,
    private val quoteMetadataBatchResultService: QuoteMetadataBatchResultService,
) {
    @Transactional
    fun prepareBatch(limit: Int): PreparedQuoteMetadataBatch {
        quoteMetadataBatchService.validateNoRunningJob()

        val pendingQuotes = quoteMetadataBatchService.getPendingQuotes(limit = limit)
        if (pendingQuotes.isEmpty()) {
            throw CustomException(ErrorCode.QUOTE_METADATA_BATCH_TARGET_NOT_FOUND)
        }

        val jobId =
            quoteMetadataBatchService.createPreparingQuoteMetadataBatchJob(
                submittedCount = pendingQuotes.size,
            )

        quoteMetadataBatchService.createQuoteMetadataBatchItem(
            jobId = jobId,
            quoteIds = pendingQuotes.map { quote -> quote.id },
            status = BatchItemStatus.PREPARING,
        )

        return PreparedQuoteMetadataBatch(
            jobId = jobId,
            quotes = pendingQuotes,
        )
    }

    @Transactional
    fun markBatchSubmitted(
        jobId: Long,
        batch: OpenAiBatchResponse,
        inputFile: OpenAiFileResponse,
    ) {
        quoteMetadataBatchService.markQuoteMetadataBatchSubmitted(
            jobId = jobId,
            batch = batch,
            inputFile = inputFile,
        )
    }

    @Transactional
    fun markBatchFailed(
        jobId: Long,
        errorMessage: String?,
    ) {
        quoteMetadataBatchService.markQuoteMetadataBatchFailed(
            jobId = jobId,
            errorMessage = errorMessage,
        )
    }

    @Transactional
    fun syncBatchStatus(
        jobId: Long,
        batch: OpenAiBatchStatusResponse,
    ) {
        quoteMetadataBatchStatusService.updateQuoteMetadataBatchJobStatus(
            jobId = jobId,
            batch = batch,
        )
    }

    @Transactional
    fun saveBatchResults(
        jobId: Long,
        results: List<ParsedBatchQuoteResult>,
    ) {
        quoteMetadataBatchResultService.saveBatchResults(
            jobId = jobId,
            results = results,
        )
    }
}

data class PreparedQuoteMetadataBatch(
    val jobId: Long,
    val quotes: List<Quote>,
)
