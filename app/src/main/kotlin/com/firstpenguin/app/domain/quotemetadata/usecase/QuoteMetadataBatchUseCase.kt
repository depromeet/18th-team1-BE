package com.firstpenguin.app.domain.quotemetadata.usecase

import com.firstpenguin.app.domain.quotemetadata.dto.QuoteMetadataBatchStatusResponse
import com.firstpenguin.app.domain.quotemetadata.dto.QuoteMetadataBatchSubmitRequest
import com.firstpenguin.app.domain.quotemetadata.dto.QuoteMetadataBatchSubmitResponse
import com.firstpenguin.app.domain.quotemetadata.service.QuoteMetadataBatchStatusService
import com.firstpenguin.app.global.security.AdminBatchSecretValidator
import org.springframework.stereotype.Component

@Component
class QuoteMetadataBatchUseCase(
    private val adminBatchSecretValidator: AdminBatchSecretValidator,
    private val quoteMetadataBatchStatusService: QuoteMetadataBatchStatusService,
    private val quoteMetadataBatchSubmitProcessor: QuoteMetadataBatchSubmitProcessor,
    private val quoteMetadataBatchSyncProcessor: QuoteMetadataBatchSyncProcessor,
) {
    fun submitBatch(
        adminSecret: String?,
        request: QuoteMetadataBatchSubmitRequest,
    ): QuoteMetadataBatchSubmitResponse {
        adminBatchSecretValidator.validate(adminSecret)

        return quoteMetadataBatchSubmitProcessor.submit(limit = request.limit)
    }

    fun getTotalStatus(adminSecret: String?): QuoteMetadataBatchStatusResponse {
        adminBatchSecretValidator.validate(adminSecret)
        quoteMetadataBatchSyncProcessor.syncActiveJobStatus()

        return quoteMetadataBatchStatusService.getTotalStatus()
    }

    fun getStatus(
        adminSecret: String?,
        jobId: Long,
    ): QuoteMetadataBatchStatusResponse {
        adminBatchSecretValidator.validate(adminSecret)
        quoteMetadataBatchSyncProcessor.syncJobStatus(jobId)

        return quoteMetadataBatchStatusService.getStatus(jobId)
    }

    fun syncBatchResult(
        adminSecret: String?,
        jobId: Long,
    ): QuoteMetadataBatchStatusResponse {
        adminBatchSecretValidator.validate(adminSecret)
        quoteMetadataBatchSyncProcessor.syncBatchResultIfReady(jobId)

        return quoteMetadataBatchStatusService.getStatus(jobId)
    }
}
