package com.firstpenguin.app.domain.quotecreation.extraction.usecase

import com.firstpenguin.app.domain.quotebatch.dto.QuoteBatchSubmitRequest
import com.firstpenguin.app.domain.quotebatch.dto.QuoteBatchSubmitResponse
import com.firstpenguin.app.global.security.AdminBatchSecretValidator
import org.springframework.stereotype.Component

@Component
class QuoteExtractionBatchUseCase(
    private val adminBatchSecretValidator: AdminBatchSecretValidator,
    private val submitProcessor: QuoteExtractionBatchSubmitProcessor,
) {
    fun submitBatch(
        adminSecret: String?,
        request: QuoteBatchSubmitRequest,
    ): QuoteBatchSubmitResponse {
        adminBatchSecretValidator.validate(adminSecret)
        return submitProcessor.submit(limit = request.limit)
    }
}
