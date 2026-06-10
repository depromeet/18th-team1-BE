package com.firstpenguin.app.domain.quotemetadata.service

import com.firstpenguin.app.domain.emotion.repository.TagRepository
import com.firstpenguin.app.domain.quotebatch.model.QuoteBatchJob
import com.firstpenguin.app.domain.quotebatch.model.QuoteBatchType
import com.firstpenguin.app.domain.quotebatch.repository.QuoteBatchJobRepository
import com.firstpenguin.app.domain.quotemetadata.dto.ParsedBatchQuoteResult
import com.firstpenguin.app.domain.quotemetadata.repository.QuoteMetadataBatchItemRepository
import com.firstpenguin.app.domain.quotemetadata.repository.QuoteMetadataRepository
import com.firstpenguin.app.global.enums.BatchItemStatus
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service

@Service
class QuoteMetadataBatchResultService(
    private val quoteMetadataRepository: QuoteMetadataRepository,
    private val quoteBatchJobRepository: QuoteBatchJobRepository,
    private val quoteMetadataBatchItemRepository: QuoteMetadataBatchItemRepository,
    private val tagRepository: TagRepository,
) {
    fun saveBatchResults(
        jobId: Long,
        results: List<ParsedBatchQuoteResult>,
    ) {
        val job = findJob(jobId)
        val tagIdByCode = getTagIdByCode()
        val statuses = results.map { result -> saveBatchResult(job, result, tagIdByCode) }

        quoteBatchJobRepository.updateQuoteBatchJobCounts(
            jobId = jobId,
            succeededCount = statuses.count { status -> status == BatchItemStatus.SUCCEEDED },
            failedCount = statuses.count { status -> status == BatchItemStatus.FAILED },
        )
    }

    private fun saveBatchResult(
        job: QuoteBatchJob,
        result: ParsedBatchQuoteResult,
        tagIdByCode: Map<String, Long>,
    ): BatchItemStatus =
        when {
            result.errorMessage != null -> {
                saveFailedBatchResult(
                    jobId = job.id,
                    customId = result.customId,
                    errorMessage = result.errorMessage,
                )
            }

            else -> {
                saveParsedBatchResult(job, result, tagIdByCode)
            }
        }

    private fun saveParsedBatchResult(
        job: QuoteBatchJob,
        result: ParsedBatchQuoteResult,
        tagIdByCode: Map<String, Long>,
    ): BatchItemStatus =
        runCatching {
            saveSucceededBatchResult(job, result, tagIdByCode)
        }.getOrElse { exception ->
            saveFailedBatchResult(
                jobId = job.id,
                customId = result.customId,
                errorMessage = exception.message,
            )
        }

    private fun saveFailedBatchResult(
        jobId: Long,
        customId: String,
        errorMessage: String?,
    ): BatchItemStatus {
        updateBatchItemStatus(
            jobId = jobId,
            customId = customId,
            status = BatchItemStatus.FAILED,
            errorMessage = errorMessage,
        )

        return BatchItemStatus.FAILED
    }

    private fun updateBatchItemStatus(
        jobId: Long,
        customId: String,
        status: BatchItemStatus,
        errorMessage: String? = null,
    ) = quoteMetadataBatchItemRepository.updateQuoteMetadataBatchItemStatus(
        jobId = jobId,
        customId = customId,
        status = status,
        errorMessage = errorMessage,
    )

    private fun saveSucceededBatchResult(
        job: QuoteBatchJob,
        result: ParsedBatchQuoteResult,
        tagIdByCode: Map<String, Long>,
    ): BatchItemStatus {
        val metadata = result.toQuoteMetadata(job.model, job.version)
        val metadataId = quoteMetadataRepository.upsertQuoteMetadata(metadata)
        val tags = result.toQuoteMetadataTags(metadataId, tagIdByCode)

        quoteMetadataRepository.replaceQuoteMetadataTags(metadataId, tags)

        updateBatchItemStatus(
            jobId = job.id,
            customId = result.customId,
            status = BatchItemStatus.SUCCEEDED,
        )

        return BatchItemStatus.SUCCEEDED
    }

    private fun findJob(jobId: Long): QuoteBatchJob =
        quoteBatchJobRepository.findByIdAndJobType(jobId, QUOTE_METADATA_JOB_TYPES)
            ?: throw CustomException(ErrorCode.QUOTE_METADATA_BATCH_TARGET_NOT_FOUND)

    private fun getTagIdByCode(): Map<String, Long> =
        tagRepository
            .getActiveTagsByType()
            .values
            .flatten()
            .associate { tag -> tag.code to tag.id }

    private companion object {
        val QUOTE_METADATA_JOB_TYPES = listOf(QuoteBatchType.QUOTE_METADATA)
    }
}
