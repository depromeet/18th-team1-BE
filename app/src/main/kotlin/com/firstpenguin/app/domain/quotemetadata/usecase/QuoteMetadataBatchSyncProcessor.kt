package com.firstpenguin.app.domain.quotemetadata.usecase

import com.firstpenguin.app.domain.embedding.usecase.QuoteEmbeddingBulkProcessor
import com.firstpenguin.app.domain.openai.dto.OpenAiBatchStatusResponse
import com.firstpenguin.app.domain.openai.service.OpenAiBatchClient
import com.firstpenguin.app.domain.quotemetadata.model.QuoteMetadataBatchJob
import com.firstpenguin.app.domain.quotemetadata.service.QuoteMetadataBatchOutputParser
import com.firstpenguin.app.domain.quotemetadata.service.QuoteMetadataBatchStatusService
import com.firstpenguin.app.global.enums.BatchJobStatus
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Component

@Component
class QuoteMetadataBatchSyncProcessor(
    private val quoteMetadataBatchStatusService: QuoteMetadataBatchStatusService,
    private val quoteMetadataBatchCommandUseCase: QuoteMetadataBatchCommandUseCase,
    private val openAiBatchClient: OpenAiBatchClient,
    private val quoteMetadataBatchOutputParser: QuoteMetadataBatchOutputParser,
    private val quoteEmbeddingBulkProcessor: QuoteEmbeddingBulkProcessor,
) {
    fun syncActiveJobStatus() {
        val activeJob = quoteMetadataBatchStatusService.getActiveJob() ?: return
        syncJobStatus(activeJob)
    }

    fun syncBatchResultIfReady(jobId: Long) {
        val job = findJob(jobId)
        val batch = syncJobStatus(job)
        val outputFileId = batch?.completedOutputFileId() ?: return
        syncCompletedBatchResult(job.id, outputFileId)
    }

    private fun findJob(jobId: Long): QuoteMetadataBatchJob =
        quoteMetadataBatchStatusService.getJob(jobId)
            ?: throw CustomException(ErrorCode.QUOTE_METADATA_BATCH_TARGET_NOT_FOUND)

    private fun syncJobStatus(job: QuoteMetadataBatchJob): OpenAiBatchStatusResponse? =
        job.openAiBatchId?.let { batchId ->
            syncOpenAiBatchStatus(job.id, batchId)
        }

    private fun syncOpenAiBatchStatus(
        jobId: Long,
        batchId: String,
    ): OpenAiBatchStatusResponse {
        val batch = fetchOpenAiBatchStatus(batchId)
        quoteMetadataBatchCommandUseCase.syncBatchStatus(jobId, batch)
        return batch
    }

    private fun fetchOpenAiBatchStatus(batchId: String): OpenAiBatchStatusResponse =
        runCatching {
            openAiBatchClient.getStatus(batchId)
        }.getOrElse { exception ->
            throw exception.toQuoteMetadataBatchException()
        }

    private fun syncCompletedBatchResult(
        jobId: Long,
        outputFileId: String,
    ) {
        val outputJsonl = openAiBatchClient.fetchBatchOutputJsonl(outputFileId)
        val parsedResults = quoteMetadataBatchOutputParser.parseBatchOutputJsonl(outputJsonl)
        quoteMetadataBatchCommandUseCase.saveBatchResults(jobId, parsedResults)
        quoteEmbeddingBulkProcessor.embedMetadataByJobId(jobId)
    }

    private fun OpenAiBatchStatusResponse.completedOutputFileId(): String? {
        if (status != BatchJobStatus.COMPLETED) {
            return null
        }
        return outputFileId
    }
}
