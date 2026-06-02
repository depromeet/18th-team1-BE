package com.firstpenguin.app.domain.batch.controller

import com.firstpenguin.app.domain.batch.dto.QuoteMetadataBatchStatusResponse
import com.firstpenguin.app.domain.batch.dto.QuoteMetadataBatchSubmitRequest
import com.firstpenguin.app.domain.batch.dto.QuoteMetadataBatchSubmitResponse
import com.firstpenguin.app.domain.batch.usecase.QuoteMetadataBatchUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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

    @Operation(
        summary = "현재 진행 중인 문장 메타정보 처리 현황 및 배치 상태 조회 API",
        description = "진행 중인 OpenAI 배치 상태를 조회해 DB 상태를 갱신한 뒤 문장 메타정보 처리 현황을 보여준다.",
    )
    @GetMapping("/quote-metadata/status")
    fun getStatus(
        @RequestHeader(ADMIN_BATCH_SECRET_HEADER, required = false) adminSecret: String?,
    ): ResponseEntity<QuoteMetadataBatchStatusResponse> =
        ResponseEntity.ok(
            quoteMetadataBatchUseCase.getStatus(adminSecret = adminSecret),
        )

    @Operation(
        summary = "문장 메타정보 배치 결과 저장 API",
        description = "완료된 OpenAI 배치 결과를 파싱해 문장 메타정보와 태그를 저장하고 처리 상태를 갱신한다.",
    )
    @GetMapping("/quote-metadata/{jobId}/sync-result")
    fun saveQuoteMetadataBatchResult(
        @PathVariable jobId: Long,
        @RequestHeader(ADMIN_BATCH_SECRET_HEADER, required = false) adminSecret: String?,
    ): ResponseEntity<QuoteMetadataBatchStatusResponse> =
        ResponseEntity.ok(
            quoteMetadataBatchUseCase.syncBatchResult(
                adminSecret = adminSecret,
                jobId = jobId,
            ),
        )
}
