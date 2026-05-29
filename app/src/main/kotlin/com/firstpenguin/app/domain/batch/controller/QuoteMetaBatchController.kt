package com.firstpenguin.app.domain.batch.controller

import com.firstpenguin.app.domain.batch.dto.QuoteMetadataBatchSubmitRequest
import com.firstpenguin.app.domain.batch.dto.QuoteMetadataBatchSubmitResponse
import com.firstpenguin.app.domain.batch.usecase.QuoteMetadataBatchUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private const val ADMIN_BATCH_SECRET_HEADER = "X-Admin-Batch-Secret"

@RestController
@RequestMapping("/admin/batches")
@Tag(name = "배치", description = "인용구 메타데이터 배치 API")
class QuoteMetaBatchController(
    private val quoteMetadataBatchUseCase: QuoteMetadataBatchUseCase,
) {
    @Operation(
        summary = "문장 메타정보 배치 등록 API",
        description = "메타정보가 존재하지 않는 문장들을 입력받은 개수만큼 llm 분석 배치 등록한다.",
    )
    @PostMapping("/quote-metadata/submit")
    fun submit(
        @RequestHeader(ADMIN_BATCH_SECRET_HEADER, required = false) adminSecret: String?,
        @Valid @RequestBody request: QuoteMetadataBatchSubmitRequest,
    ): ResponseEntity<QuoteMetadataBatchSubmitResponse> =
        ResponseEntity.ok(
            quoteMetadataBatchUseCase.submitBatch(
                adminSecret = adminSecret,
                request = request,
            ),
        )
}
