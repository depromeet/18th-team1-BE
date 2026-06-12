package com.firstpenguin.app.domain.quotecreation.review.controller

import com.firstpenguin.app.domain.quotebatch.dto.QuoteBatchSubmitRequest
import com.firstpenguin.app.domain.quotebatch.dto.QuoteBatchSubmitResponse
import com.firstpenguin.app.domain.quotecreation.review.usecase.QuoteReviewBatchUseCase
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
@RequestMapping("/admin/batches/quote-creation/review")
@Tag(name = "추천 문장 후보 검수 배치", description = "수집된 추천 문장 후보를 검수하는 배치 API")
class QuoteReviewBatchController(
    private val quoteReviewBatchUseCase: QuoteReviewBatchUseCase,
) {
    @Operation(
        summary = "책별 추천 문장 후보 검수 배치 등록 API",
        description = "검수 대기 후보가 있는 책을 입력받은 개수만큼 OpenAI 후보 검수 배치로 등록한다.",
    )
    @PostMapping("/submit")
    fun submitQuoteReviewBatch(
        @RequestHeader(ADMIN_BATCH_SECRET_HEADER, required = false) adminSecret: String?,
        @Valid @RequestBody request: QuoteBatchSubmitRequest,
    ): ResponseEntity<QuoteBatchSubmitResponse> =
        ResponseEntity.ok(
            quoteReviewBatchUseCase.submitBatch(
                adminSecret = adminSecret,
                request = request,
            ),
        )
}
