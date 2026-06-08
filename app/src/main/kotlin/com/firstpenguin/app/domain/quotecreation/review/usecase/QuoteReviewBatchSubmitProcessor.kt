package com.firstpenguin.app.domain.quotecreation.review.usecase

import com.firstpenguin.app.domain.openai.dto.OpenAiBatchResponse
import com.firstpenguin.app.domain.openai.dto.OpenAiFileResponse
import com.firstpenguin.app.domain.openai.service.OpenAiBatchClient
import com.firstpenguin.app.domain.quotebatch.dto.QuoteBatchSubmitResponse
import com.firstpenguin.app.domain.quotecreation.review.service.QuoteReviewBatchJsonlBuilder
import com.firstpenguin.app.domain.quotecreation.usecase.PreparedCandidateReviewBatch
import com.firstpenguin.app.domain.quotecreation.usecase.QuoteCreationBatchCommandUseCase
import com.firstpenguin.app.domain.quotecreation.usecase.toQuoteCreationBatchException
import org.springframework.stereotype.Component

@Component
class QuoteReviewBatchSubmitProcessor(
    private val quoteReviewBatchJsonlBuilder: QuoteReviewBatchJsonlBuilder,
    private val openAiBatchClient: OpenAiBatchClient,
    private val commandUseCase: QuoteCreationBatchCommandUseCase,
) {
    fun submit(limit: Int): QuoteBatchSubmitResponse {
        val preparedBatch = commandUseCase.prepareCandidateReviewBatch(limit = limit)
        return submitPreparedBatch(preparedBatch)
    }

    private fun submitPreparedBatch(preparedBatch: PreparedCandidateReviewBatch): QuoteBatchSubmitResponse =
        runCatching {
            createSubmittedBatch(preparedBatch)
        }.getOrElse { exception ->
            throw markPreparedBatchFailed(preparedBatch.jobId, exception)
        }

    private fun createSubmittedBatch(preparedBatch: PreparedCandidateReviewBatch): QuoteBatchSubmitResponse {
        val inputJsonl = quoteReviewBatchJsonlBuilder.build(preparedBatch.targets)
        val inputFile = openAiBatchClient.uploadBatchInput(inputJsonl)
        val batch = openAiBatchClient.createBatch(inputFile.id)
        markBatchSubmitted(preparedBatch.jobId, batch, inputFile)
        return QuoteBatchSubmitResponse(preparedBatch.jobId, batch.id)
    }

    private fun markBatchSubmitted(
        jobId: Long,
        batch: OpenAiBatchResponse,
        inputFile: OpenAiFileResponse,
    ) = commandUseCase.markBatchSubmitted(jobId, batch, inputFile)

    private fun markPreparedBatchFailed(
        jobId: Long,
        exception: Throwable,
    ): Throwable {
        runCatching { commandUseCase.markBatchFailed(jobId, exception.message) }
        return exception.toQuoteCreationBatchException()
    }
}
