package com.firstpenguin.app.domain.quotecreation.usecase

import com.firstpenguin.app.domain.quotecreation.dto.QuoteCreationBatchStatusResponse
import com.firstpenguin.app.domain.quotecreation.service.QuoteCreationBatchStatusService
import com.firstpenguin.app.global.security.AdminBatchSecretValidator
import org.springframework.stereotype.Component

@Component
class QuoteCreationBatchUseCase(
    private val adminBatchSecretValidator: AdminBatchSecretValidator,
    private val statusService: QuoteCreationBatchStatusService,
    private val syncProcessor: QuoteCreationBatchSyncProcessor,
) {
    fun getTotalStatus(adminSecret: String?): QuoteCreationBatchStatusResponse {
        adminBatchSecretValidator.validate(adminSecret)
        syncProcessor.syncActiveJobStatus()
        return statusService.getTotalStatus()
    }

    fun getStatus(
        adminSecret: String?,
        jobId: Long,
    ): QuoteCreationBatchStatusResponse {
        adminBatchSecretValidator.validate(adminSecret)
        syncProcessor.syncJobStatus(jobId)
        return statusService.getStatus(jobId)
    }

    fun syncBatchResult(
        adminSecret: String?,
        jobId: Long,
    ): QuoteCreationBatchStatusResponse {
        adminBatchSecretValidator.validate(adminSecret)
        syncProcessor.syncBatchResultIfReady(jobId)
        return statusService.getStatus(jobId)
    }
}
