package com.firstpenguin.app.domain.quotemetadata.usecase

import com.firstpenguin.app.domain.embedding.usecase.QuoteEmbeddingBulkProcessor
import com.firstpenguin.app.domain.openai.dto.OpenAiBatchStatusResponse
import com.firstpenguin.app.domain.openai.service.OpenAiBatchClient
import com.firstpenguin.app.domain.quotebatch.model.QuoteBatchJob
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
        syncCompletedBatchIfReady(activeJob)
    }

    fun syncBatchResultIfReady(jobId: Long) {
        val job = findJob(jobId)
        syncCompletedBatchIfReady(job)
    }

    private fun findJob(jobId: Long): QuoteBatchJob =
        quoteMetadataBatchStatusService.getJob(jobId)
            ?: throw CustomException(ErrorCode.QUOTE_METADATA_BATCH_JOB_NOT_FOUND)

    private fun syncJobStatus(job: QuoteBatchJob): OpenAiBatchStatusResponse? =
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

    private fun syncCompletedBatchIfReady(job: QuoteBatchJob) {
        val batch = syncJobStatus(job) ?: return
        val fileIds = batch.completedResultFileIds()
        if (fileIds.isEmpty()) return
        syncCompletedBatchResult(job.id, fileIds)
    }

    private fun syncCompletedBatchResult(
        jobId: Long,
        fileIds: List<String>,
    ) {
        val parsedResults = fileIds.flatMap { fileId -> parseBatchResultFile(fileId) }
        if (parsedResults.isEmpty()) return
        quoteMetadataBatchCommandUseCase.saveBatchResults(jobId, parsedResults)
        quoteEmbeddingBulkProcessor.embedMetadataByJobId(jobId)
    }

    private fun parseBatchResultFile(fileId: String) =
        openAiBatchClient
            .fetchBatchOutputJsonl(fileId)
            .let { jsonl -> quoteMetadataBatchOutputParser.parseBatchOutputJsonl(jsonl) }

    private fun OpenAiBatchStatusResponse.completedResultFileIds(): List<String> {
        if (status != BatchJobStatus.COMPLETED) return emptyList()
        return listOfNotNull(outputFileId, errorFileId)
    }
}
