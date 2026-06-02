package com.firstpenguin.app.domain.batch.usecase

import com.firstpenguin.app.domain.batch.dto.ParsedBatchQuoteResult
import com.firstpenguin.app.domain.batch.dto.ai.OpenAiBatchResponse
import com.firstpenguin.app.domain.batch.dto.ai.OpenAiBatchStatusResponse
import com.firstpenguin.app.domain.batch.dto.ai.OpenAiFileResponse
import com.firstpenguin.app.domain.batch.service.QuoteMetadataService
import com.firstpenguin.app.domain.quote.model.Quote
import com.firstpenguin.app.global.enums.BatchItemStatus
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class QuoteMetadataBatchCommandUseCase(
    private val quoteMetadataService: QuoteMetadataService,
) {
    @Transactional
    fun prepareBatch(limit: Int): PreparedQuoteMetadataBatch {
        quoteMetadataService.validateNoRunningJob()

        val pendingQuotes = quoteMetadataService.getPendingQuotes(limit = limit)
        if (pendingQuotes.isEmpty()) {
            throw CustomException(ErrorCode.QUOTE_METADATA_BATCH_TARGET_NOT_FOUND)
        }

        val jobId =
            quoteMetadataService.createPreparingQuoteMetadataBatchJob(
                submittedCount = pendingQuotes.size,
            )

        quoteMetadataService.createQuoteMetadataBatchItem(
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
        quoteMetadataService.markQuoteMetadataBatchSubmitted(
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
        quoteMetadataService.markQuoteMetadataBatchFailed(
            jobId = jobId,
            errorMessage = errorMessage,
        )
    }

    @Transactional
    fun syncBatchStatus(
        jobId: Long,
        batch: OpenAiBatchStatusResponse,
    ) {
        quoteMetadataService.updateQuoteMetadataBatchJobStatus(
            jobId = jobId,
            batch = batch,
        )
    }

    @Transactional
    fun saveBatchResults(
        jobId: Long,
        results: List<ParsedBatchQuoteResult>,
    ) {
        quoteMetadataService.saveBatchResults(
            jobId = jobId,
            results = results,
        )
    }
}

data class PreparedQuoteMetadataBatch(
    val jobId: Long,
    val quotes: List<Quote>,
)
