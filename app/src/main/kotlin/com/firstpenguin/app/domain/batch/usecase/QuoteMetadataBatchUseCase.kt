package com.firstpenguin.app.domain.batch.usecase

import com.firstpenguin.app.domain.batch.dto.QuoteMetadataBatchStatusResponse
import com.firstpenguin.app.domain.batch.dto.QuoteMetadataBatchSubmitRequest
import com.firstpenguin.app.domain.batch.dto.QuoteMetadataBatchSubmitResponse
import com.firstpenguin.app.domain.batch.dto.ai.OpenAiBatchStatusResponse
import com.firstpenguin.app.domain.batch.model.QuoteMetadataBatchJob
import com.firstpenguin.app.domain.batch.service.AdminBatchSecretValidator
import com.firstpenguin.app.domain.batch.service.OpenAiBatchClient
import com.firstpenguin.app.domain.batch.service.OutputJsonlParser
import com.firstpenguin.app.domain.batch.service.QuoteMetadataBatchStatusService
import com.firstpenguin.app.global.enums.BatchJobStatus
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Component

@Component
class QuoteMetadataBatchUseCase(
    private val adminBatchSecretValidator: AdminBatchSecretValidator,
    private val quoteMetadataBatchStatusService: QuoteMetadataBatchStatusService,
    private val quoteMetadataBatchSubmitProcessor: QuoteMetadataBatchSubmitProcessor,
    private val openAiBatchClient: OpenAiBatchClient,
    private val outputJsonlParser: OutputJsonlParser,
    private val quoteMetadataBatchCommandUseCase: QuoteMetadataBatchCommandUseCase,
) {
    fun submitBatch(
        adminSecret: String?,
        request: QuoteMetadataBatchSubmitRequest,
    ): QuoteMetadataBatchSubmitResponse {
        adminBatchSecretValidator.validate(adminSecret)
        val preparedBatch = quoteMetadataBatchCommandUseCase.prepareBatch(limit = request.limit)

        return quoteMetadataBatchSubmitProcessor.submit(preparedBatch)
    }

    fun getStatus(adminSecret: String?): QuoteMetadataBatchStatusResponse {
        adminBatchSecretValidator.validate(adminSecret)
        syncActiveJobStatus()

        return quoteMetadataBatchStatusService.getStatus()
    }

    fun syncBatchResult(
        adminSecret: String?,
        jobId: Long,
    ): QuoteMetadataBatchStatusResponse {
        adminBatchSecretValidator.validate(adminSecret)
        syncResultIfReady(findJob(jobId))

        return quoteMetadataBatchStatusService.getStatus()
    }

    private fun findJob(jobId: Long): QuoteMetadataBatchJob =
        quoteMetadataBatchStatusService.getJob(jobId)
            ?: throw CustomException(ErrorCode.QUOTE_METADATA_BATCH_TARGET_NOT_FOUND)

    private fun syncResultIfReady(job: QuoteMetadataBatchJob) {
        val batch = syncJobStatus(job)
        if (batch != null && batch.status == BatchJobStatus.COMPLETED && batch.outputFileId != null) {
            syncCompletedBatchResult(job.id, batch)
        }
    }

    private fun syncCompletedBatchResult(
        jobId: Long,
        batch: OpenAiBatchStatusResponse,
    ) {
        val outputJsonl = openAiBatchClient.fetchBatchOutputJsonl(requireNotNull(batch.outputFileId))
        val parsedResults = outputJsonlParser.parseBatchOutputJsonl(outputJsonl)

        quoteMetadataBatchCommandUseCase.saveBatchResults(
            jobId = jobId,
            results = parsedResults,
        )
    }

    private fun syncActiveJobStatus() {
        val activeJob = quoteMetadataBatchStatusService.getActiveJob() ?: return
        syncJobStatus(activeJob)
    }

    private fun syncJobStatus(job: QuoteMetadataBatchJob): OpenAiBatchStatusResponse? =
        job.openAiBatchId?.let { batchId ->
            syncOpenAiBatchStatus(job.id, batchId)
        }

    private fun syncOpenAiBatchStatus(
        jobId: Long,
        batchId: String,
    ): OpenAiBatchStatusResponse {
        val batch = fetchOpenAiBatchStatus(batchId)
        quoteMetadataBatchCommandUseCase.syncBatchStatus(
            jobId = jobId,
            batch = batch,
        )

        return batch
    }

    private fun fetchOpenAiBatchStatus(batchId: String): OpenAiBatchStatusResponse =
        runCatching {
            openAiBatchClient.getStatus(batchId)
        }.getOrElse { exception ->
            throw exception.toOpenAiBatchException()
        }
}
