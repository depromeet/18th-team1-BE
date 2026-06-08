package com.firstpenguin.app.domain.quotecreation.extraction.controller

import com.firstpenguin.app.domain.quotebatch.dto.QuoteBatchSubmitRequest
import com.firstpenguin.app.domain.quotebatch.dto.QuoteBatchSubmitResponse
import com.firstpenguin.app.domain.quotecreation.extraction.usecase.QuoteExtractionBatchUseCase
import com.firstpenguin.app.global.security.ADMIN_BATCH_SECRET_HEADER
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/batches/quote-creation/extraction")
@Tag(name = "추천 문장 추출 배치", description = "책 정보를 기반으로 추천 문장을 추출하는 배치 API")
class QuoteExtractionBatchController(
    private val quoteExtractionBatchUseCase: QuoteExtractionBatchUseCase,
) {
    @Operation(
        summary = "책별 추천 문장 추출 배치 등록 API",
        description = "추천 문장이 부족하고 검수 대기 후보가 없는 책을 OpenAI 추천 문장 추출 배치로 등록한다.",
    )
    @PostMapping("/submit")
    fun submitQuoteExtractionBatch(
        @RequestHeader(ADMIN_BATCH_SECRET_HEADER, required = false) adminSecret: String?,
        @Valid @RequestBody request: QuoteBatchSubmitRequest,
    ): ResponseEntity<QuoteBatchSubmitResponse> =
        ResponseEntity.ok(
            quoteExtractionBatchUseCase.submitBatch(
                adminSecret = adminSecret,
                request = request,
            ),
        )
}
