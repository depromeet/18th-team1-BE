package com.firstpenguin.app.domain.batch.usecase

import com.firstpenguin.app.domain.batch.dto.QuoteMetadataBatchSubmitResponse
import com.firstpenguin.app.domain.batch.dto.ai.OpenAiBatchResponse
import com.firstpenguin.app.domain.batch.dto.ai.OpenAiFileResponse
import com.firstpenguin.app.domain.batch.service.OpenAiBatchClient
import com.firstpenguin.app.domain.batch.service.QuoteMetadataBatchJsonlBuilder
import com.firstpenguin.app.domain.batch.service.QuoteMetadataService
import org.springframework.stereotype.Component

@Component
class QuoteMetadataBatchSubmitProcessor(
    private val quoteMetadataService: QuoteMetadataService,
    private val quoteMetadataBatchJsonlBuilder: QuoteMetadataBatchJsonlBuilder,
    private val openAiBatchClient: OpenAiBatchClient,
    private val quoteMetadataBatchCommandUseCase: QuoteMetadataBatchCommandUseCase,
) {
    fun submit(preparedBatch: PreparedQuoteMetadataBatch): QuoteMetadataBatchSubmitResponse =
        runCatching {
            createSubmittedBatch(preparedBatch)
        }.getOrElse { exception ->
            throw markPreparedBatchFailed(preparedBatch.jobId, exception)
        }

    private fun createSubmittedBatch(preparedBatch: PreparedQuoteMetadataBatch): QuoteMetadataBatchSubmitResponse {
        val inputFile = openAiBatchClient.uploadBatchInput(buildBatchInput(preparedBatch))
        val batch = openAiBatchClient.createBatch(inputFile.id)

        markBatchSubmitted(preparedBatch.jobId, batch, inputFile)
        return QuoteMetadataBatchSubmitResponse(preparedBatch.jobId, batch.id)
    }

    private fun buildBatchInput(preparedBatch: PreparedQuoteMetadataBatch): String =
        quoteMetadataBatchJsonlBuilder.build(
            quotes = preparedBatch.quotes,
            tagGroups = quoteMetadataService.getAllTagsByType(),
        )

    private fun markBatchSubmitted(
        jobId: Long,
        batch: OpenAiBatchResponse,
        inputFile: OpenAiFileResponse,
    ) = quoteMetadataBatchCommandUseCase.markBatchSubmitted(jobId, batch, inputFile)

    private fun markPreparedBatchFailed(
        jobId: Long,
        exception: Throwable,
    ): Throwable {
        runCatching { quoteMetadataBatchCommandUseCase.markBatchFailed(jobId, exception.message) }
        return exception.toOpenAiBatchException()
    }
}
