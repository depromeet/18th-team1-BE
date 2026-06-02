package com.firstpenguin.app.domain.batch.service

import com.firstpenguin.app.domain.batch.dto.ParsedBatchQuoteResult
import com.firstpenguin.app.domain.batch.repository.QuoteMetadataBatchItemRepository
import com.firstpenguin.app.domain.batch.repository.QuoteMetadataBatchJobRepository
import com.firstpenguin.app.domain.batch.repository.QuoteMetadataRepository
import com.firstpenguin.app.domain.emotion.repository.TagRepository
import com.firstpenguin.app.global.enums.BatchItemStatus
import com.firstpenguin.app.global.enums.QuoteMetadataBatchModelVersion
import org.springframework.stereotype.Service

@Service
class QuoteMetadataBatchResultService(
    private val quoteMetadataRepository: QuoteMetadataRepository,
    private val quoteMetadataBatchJobRepository: QuoteMetadataBatchJobRepository,
    private val quoteMetadataBatchItemRepository: QuoteMetadataBatchItemRepository,
    private val tagRepository: TagRepository,
) {
    fun saveBatchResults(
        jobId: Long,
        results: List<ParsedBatchQuoteResult>,
    ) {
        val tagIdByCode = getTagIdByCode()
        val statuses = results.map { result -> saveBatchResult(jobId, result, tagIdByCode) }

        quoteMetadataBatchJobRepository.updateQuoteMetadataBatchJobCounts(
            jobId = jobId,
            succeededCount = statuses.count { status -> status == BatchItemStatus.SUCCEEDED },
            failedCount = statuses.count { status -> status == BatchItemStatus.FAILED },
        )
    }

    private fun saveBatchResult(
        jobId: Long,
        result: ParsedBatchQuoteResult,
        tagIdByCode: Map<String, Long>,
    ): BatchItemStatus =
        when {
            result.quoteId == null -> BatchItemStatus.FAILED
            result.errorMessage != null -> saveFailedBatchResult(jobId, result.quoteId, result.errorMessage)
            else -> saveParsedBatchResult(jobId, result, tagIdByCode)
        }

    private fun saveParsedBatchResult(
        jobId: Long,
        result: ParsedBatchQuoteResult,
        tagIdByCode: Map<String, Long>,
    ): BatchItemStatus =
        runCatching {
            saveSucceededBatchResult(jobId, result, tagIdByCode)
        }.getOrElse { exception ->
            saveFailedBatchResult(jobId, requireNotNull(result.quoteId), exception.message)
        }

    private fun saveSucceededBatchResult(
        jobId: Long,
        result: ParsedBatchQuoteResult,
        tagIdByCode: Map<String, Long>,
    ): BatchItemStatus {
        val metadata = result.toQuoteMetadata(QuoteMetadataBatchModelVersion.V1)
        val metadataId = quoteMetadataRepository.upsertQuoteMetadata(metadata)
        val tags = result.toQuoteMetadataTags(metadataId, tagIdByCode)

        quoteMetadataRepository.replaceQuoteMetadataTags(metadataId, tags)
        updateBatchItemStatus(jobId, requireNotNull(result.quoteId), BatchItemStatus.SUCCEEDED)

        return BatchItemStatus.SUCCEEDED
    }

    private fun saveFailedBatchResult(
        jobId: Long,
        quoteId: Long,
        errorMessage: String?,
    ): BatchItemStatus {
        updateBatchItemStatus(jobId, quoteId, BatchItemStatus.FAILED, errorMessage)
        return BatchItemStatus.FAILED
    }

    private fun updateBatchItemStatus(
        jobId: Long,
        quoteId: Long,
        status: BatchItemStatus,
        errorMessage: String? = null,
    ) = quoteMetadataBatchItemRepository.updateQuoteMetadataBatchItemStatus(
        jobId = jobId,
        quoteId = quoteId,
        status = status,
        errorMessage = errorMessage,
    )

    private fun getTagIdByCode(): Map<String, Long> =
        tagRepository
            .getActiveTagsByType()
            .values
            .flatten()
            .associate { tag -> tag.code to tag.id }
}
