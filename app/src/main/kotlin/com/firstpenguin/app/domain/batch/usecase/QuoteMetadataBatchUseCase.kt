package com.firstpenguin.app.domain.batch.usecase

import com.firstpenguin.app.domain.batch.dto.QuoteMetadataBatchStatusResponse
import com.firstpenguin.app.domain.batch.dto.QuoteMetadataBatchSubmitRequest
import com.firstpenguin.app.domain.batch.dto.QuoteMetadataBatchSubmitResponse
import com.firstpenguin.app.domain.batch.service.AdminBatchSecretValidator
import com.firstpenguin.app.domain.batch.service.OpenAiBatchClient
import com.firstpenguin.app.domain.batch.service.QuoteMetadataBatchJsonlBuilder
import com.firstpenguin.app.domain.batch.service.QuoteMetadataService
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientResponseException

@Component
class QuoteMetadataBatchUseCase(
    private val adminBatchSecretValidator: AdminBatchSecretValidator,
    private val quoteMetadataService: QuoteMetadataService,
    private val quoteMetadataBatchJsonlBuilder: QuoteMetadataBatchJsonlBuilder,
    private val openAiBatchClient: OpenAiBatchClient,
    private val quoteMetadataBatchCommandUseCase: QuoteMetadataBatchCommandUseCase,
) {
    fun submitBatch(
        adminSecret: String?,
        request: QuoteMetadataBatchSubmitRequest,
    ): QuoteMetadataBatchSubmitResponse {
        adminBatchSecretValidator.validate(adminSecret)

        val preparedBatch =
            quoteMetadataBatchCommandUseCase.prepareBatch(limit = request.limit)

        val tagsToMatch = quoteMetadataService.getAllTagsByType()

        val jsonl =
            quoteMetadataBatchJsonlBuilder.build(
                quotes = preparedBatch.quotes,
                tagGroups = tagsToMatch,
            )

        return runCatching {
            val inputFile = openAiBatchClient.uploadBatchInput(jsonl)
            val batch = openAiBatchClient.createBatch(inputFile.id)

            quoteMetadataBatchCommandUseCase.markBatchSubmitted(
                jobId = preparedBatch.jobId,
                batch = batch,
                inputFile = inputFile,
            )

            QuoteMetadataBatchSubmitResponse(
                jobId = preparedBatch.jobId,
                openAiBatchId = batch.id,
            )
        }.getOrElse { exception ->
            runCatching {
                quoteMetadataBatchCommandUseCase.markBatchFailed(
                    jobId = preparedBatch.jobId,
                    errorMessage = exception.message,
                )
            }
            throw exception.toOpenAiBatchException()
        }
    }

    fun getStatus(adminSecret: String?): QuoteMetadataBatchStatusResponse {
        adminBatchSecretValidator.validate(adminSecret)
        syncActiveJobStatus()

        return quoteMetadataService.getStatus()
    }

    private fun syncActiveJobStatus() {
        val activeJob = quoteMetadataService.getActiveJob() ?: return
        val openAiBatchId = activeJob.openAiBatchId ?: return

        val batch =
            runCatching {
                openAiBatchClient.getStatus(openAiBatchId)
            }.getOrElse { exception ->
                throw exception.toOpenAiBatchException()
            }

        quoteMetadataBatchCommandUseCase.syncBatchStatus(
            jobId = activeJob.id,
            batch = batch,
        )
    }

    private fun Throwable.toOpenAiBatchException(): Throwable {
        if (this is RestClientResponseException) {
            return CustomException(ErrorCode.QUOTE_METADATA_BATCH_OPENAI_REQUEST_FAILED)
        }

        return this
    }
}
