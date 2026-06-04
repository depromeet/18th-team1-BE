package com.firstpenguin.app.domain.batch.usecase

import com.firstpenguin.app.domain.batch.dto.QuoteMetadataBatchStatusResponse
import com.firstpenguin.app.domain.batch.dto.QuoteMetadataBatchSubmitRequest
import com.firstpenguin.app.domain.batch.dto.QuoteMetadataBatchSubmitResponse
import com.firstpenguin.app.domain.batch.service.AdminBatchSecretValidator
import com.firstpenguin.app.domain.batch.service.QuoteMetadataBatchStatusService
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

    fun getStatus(adminSecret: String?): QuoteMetadataBatchStatusResponse {
        adminBatchSecretValidator.validate(adminSecret)
        quoteMetadataBatchSyncProcessor.syncActiveJobStatus()

        return quoteMetadataBatchStatusService.getStatus()
    }

    fun syncBatchResult(
        adminSecret: String?,
        jobId: Long,
    ): QuoteMetadataBatchStatusResponse {
        adminBatchSecretValidator.validate(adminSecret)
        quoteMetadataBatchSyncProcessor.syncBatchResultIfReady(jobId)

        return quoteMetadataBatchStatusService.getStatus()
    }
}
