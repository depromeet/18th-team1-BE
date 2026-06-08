package com.firstpenguin.app.domain.quotecreation.usecase

import com.firstpenguin.app.domain.openai.dto.OpenAiBatchStatusResponse
import com.firstpenguin.app.domain.openai.service.OpenAiBatchClient
import com.firstpenguin.app.domain.quotebatch.model.QuoteBatchJob
import com.firstpenguin.app.domain.quotebatch.model.QuoteBatchType
import com.firstpenguin.app.domain.quotecreation.model.QuoteCreationBatchResultType
import com.firstpenguin.app.domain.quotecreation.service.QuoteCreationBatchOutputParser
import com.firstpenguin.app.domain.quotecreation.service.QuoteCreationBatchStatusService
import com.firstpenguin.app.global.enums.BatchJobStatus
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Component

@Component
class QuoteCreationBatchSyncProcessor(
    private val statusService: QuoteCreationBatchStatusService,
    private val commandUseCase: QuoteCreationBatchCommandUseCase,
    private val openAiBatchClient: OpenAiBatchClient,
    private val outputParser: QuoteCreationBatchOutputParser,
) {
    fun syncActiveJobStatus() {
        val activeJob = statusService.getActiveJob() ?: return
        syncCompletedBatchIfReady(activeJob)
    }

    fun syncBatchResultIfReady(jobId: Long) {
        val job = findJob(jobId)
        syncCompletedBatchIfReady(job)
    }

    private fun findJob(jobId: Long): QuoteBatchJob =
        statusService.getJob(jobId)
            ?: throw CustomException(ErrorCode.QUOTE_CREATION_BATCH_JOB_NOT_FOUND)

    private fun syncJobStatus(job: QuoteBatchJob): OpenAiBatchStatusResponse? =
        job.openAiBatchId?.let { batchId ->
            syncOpenAiBatchStatus(job.id, batchId)
        }

    private fun syncOpenAiBatchStatus(
        jobId: Long,
        batchId: String,
    ): OpenAiBatchStatusResponse {
        val batch = fetchOpenAiBatchStatus(batchId)
        commandUseCase.syncBatchStatus(jobId, batch)
        return batch
    }

    private fun fetchOpenAiBatchStatus(batchId: String): OpenAiBatchStatusResponse =
        runCatching {
            openAiBatchClient.getStatus(batchId)
        }.getOrElse { exception ->
            throw exception.toQuoteCreationBatchException()
        }

    private fun syncCompletedBatchIfReady(job: QuoteBatchJob) {
        val batch = syncJobStatus(job) ?: return
        val fileIds = batch.completedResultFileIds()
        if (fileIds.isEmpty()) return
        syncCompletedBatchResult(job, fileIds)
    }

    private fun syncCompletedBatchResult(
        job: QuoteBatchJob,
        fileIds: List<String>,
    ) {
        val resultType = job.jobType.toQuoteCreationBatchResultType()
        val parsedResults = fileIds.flatMap { fileId -> parseBatchResultFile(fileId, resultType) }
        if (parsedResults.isEmpty()) return
        commandUseCase.saveBatchResults(job.id, parsedResults)
    }

    private fun parseBatchResultFile(
        fileId: String,
        resultType: QuoteCreationBatchResultType,
    ) =
        openAiBatchClient
            .fetchBatchOutputJsonl(fileId)
            .let { jsonl -> outputParser.parseBatchOutputJsonl(jsonl, resultType) }

    private fun OpenAiBatchStatusResponse.completedResultFileIds(): List<String> =
        if (status == BatchJobStatus.COMPLETED) {
            listOfNotNull(outputFileId, errorFileId)
        } else {
            emptyList()
        }
}

private fun QuoteBatchType.toQuoteCreationBatchResultType(): QuoteCreationBatchResultType =
    when (this) {
        QuoteBatchType.QUOTE_EXTRACTION -> QuoteCreationBatchResultType.QUOTE_EXTRACTION
        QuoteBatchType.QUOTE_REVIEW -> QuoteCreationBatchResultType.QUOTE_REVIEW
        QuoteBatchType.QUOTE_METADATA -> throw CustomException(ErrorCode.QUOTE_CREATION_BATCH_JOB_NOT_FOUND)
    }
