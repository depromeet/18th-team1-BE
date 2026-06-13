package com.firstpenguin.app.domain.quotemetadata.controller

import com.firstpenguin.app.domain.quotemetadata.dto.QuoteMetadataBatchStatusResponse
import com.firstpenguin.app.domain.quotemetadata.dto.QuoteMetadataBatchSubmitRequest
import com.firstpenguin.app.domain.quotemetadata.dto.QuoteMetadataBatchSubmitResponse
import com.firstpenguin.app.domain.quotemetadata.usecase.QuoteMetadataBatchUseCase
import com.firstpenguin.app.global.security.ADMIN_BATCH_SECRET_HEADER
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

@RestController
@RequestMapping("/admin/batches/quote-metadata")
@Tag(name = "문장 메타정보 배치", description = "저장된 추천 문장에 메타정보와 태그를 붙이는 배치 API")
class QuoteMetadataBatchController(
    private val quoteMetadataBatchUseCase: QuoteMetadataBatchUseCase,
) {
    @Operation(
        summary = "문장 메타정보 배치 등록 API",
        description = "메타정보가 없는 추천 문장을 입력받은 개수만큼 OpenAI 메타정보 분석 배치로 등록한다.",
    )
    @PostMapping("/submit")
    fun submitQuoteMetadataBatch(
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
        summary = "문장 메타정보 배치 처리 현황 조회 API",
        description = "진행 중인 OpenAI 배치 상태를 조회해 DB 상태를 갱신한 뒤 문장 메타정보 처리 현황을 보여준다.",
    )
    @GetMapping("/status")
    fun getQuoteMetadataBatchStatus(
        @RequestHeader(ADMIN_BATCH_SECRET_HEADER, required = false) adminSecret: String?,
    ): ResponseEntity<QuoteMetadataBatchStatusResponse> =
        ResponseEntity.ok(
            quoteMetadataBatchUseCase.getTotalStatus(adminSecret = adminSecret),
        )

    @Operation(
        summary = "문장 메타정보 배치 단건 처리 현황 조회 API",
        description = "지정한 OpenAI 배치 상태를 조회해 DB 상태를 갱신한 뒤 문장 메타정보 처리 현황을 보여준다.",
    )
    @GetMapping("/{jobId}/status")
    fun getQuoteMetadataBatchStatusByJobId(
        @PathVariable jobId: Long,
        @RequestHeader(ADMIN_BATCH_SECRET_HEADER, required = false) adminSecret: String?,
    ): ResponseEntity<QuoteMetadataBatchStatusResponse> =
        ResponseEntity.ok(
            quoteMetadataBatchUseCase.getStatus(
                adminSecret = adminSecret,
                jobId = jobId,
            ),
        )

    @Operation(
        summary = "문장 메타정보 배치 결과 동기화 API",
        description = "완료된 OpenAI 배치 결과를 파싱해 문장 메타정보와 태그를 저장하고 처리 상태를 갱신한다.",
    )
    @PostMapping("/{jobId}/sync-result")
    fun syncQuoteMetadataBatchResult(
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
