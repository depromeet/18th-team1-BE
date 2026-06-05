package com.firstpenguin.app.domain.quotemetadata.service

import com.firstpenguin.app.domain.openai.dto.OpenAiBatchStatusResponse
import com.firstpenguin.app.domain.quotemetadata.dto.ActiveJobStatusResponse
import com.firstpenguin.app.domain.quotemetadata.dto.QuoteMetadataBatchStatusResponse
import com.firstpenguin.app.domain.quotemetadata.model.QuoteMetadataBatchJob
import com.firstpenguin.app.domain.quotemetadata.repository.QuoteMetadataBatchItemRepository
import com.firstpenguin.app.domain.quotemetadata.repository.QuoteMetadataBatchJobRepository
import com.firstpenguin.app.domain.quotemetadata.repository.QuoteMetadataRepository
import com.firstpenguin.app.global.enums.BatchItemStatus
import org.springframework.stereotype.Service

@Service
class QuoteMetadataBatchStatusService(
    private val quoteMetadataRepository: QuoteMetadataRepository,
    private val quoteMetadataBatchJobRepository: QuoteMetadataBatchJobRepository,
    private val quoteMetadataBatchItemRepository: QuoteMetadataBatchItemRepository,
) {
    fun getStatus(): QuoteMetadataBatchStatusResponse =
        QuoteMetadataBatchStatusResponse(
            totalQuoteCount = quoteMetadataRepository.countTotalQuotes(),
            createdCount = quoteMetadataRepository.countCreatedMetadata(),
            pendingCount = quoteMetadataRepository.countPendingQuotes(),
            processingCount = quoteMetadataBatchItemRepository.countActiveItemsWithoutMetadata(),
            failedCount = quoteMetadataBatchItemRepository.countFailedItemsWithoutMetadata(),
            runningJobCount = quoteMetadataBatchJobRepository.countRunningJobs(),
            activeJob = quoteMetadataBatchJobRepository.findActiveJob()?.toResponse(),
        )

    fun getActiveJob(): QuoteMetadataBatchJob? = quoteMetadataBatchJobRepository.findActiveJob()

    fun getJob(jobId: Long): QuoteMetadataBatchJob? = quoteMetadataBatchJobRepository.findById(jobId)

    fun updateQuoteMetadataBatchJobStatus(
        jobId: Long,
        batch: OpenAiBatchStatusResponse,
    ) {
        quoteMetadataBatchJobRepository.updateQuoteMetadataBatchJobStatus(
            jobId = jobId,
            status = batch.status,
            outputFileId = batch.outputFileId,
            errorFileId = batch.errorFileId,
        )

        if (batch.status.isFailedTerminal()) {
            quoteMetadataBatchItemRepository.updateQuoteMetadataBatchItemsStatus(
                jobId = jobId,
                status = BatchItemStatus.FAILED,
                errorMessage = "OpenAI batch status: ${batch.status.name}",
            )
        }
    }

    private fun QuoteMetadataBatchJob.toResponse(): ActiveJobStatusResponse =
        ActiveJobStatusResponse(
            jobId = id,
            openAiBatchId = openAiBatchId,
            submittedCount = submittedCount,
            status = status,
        )
}
