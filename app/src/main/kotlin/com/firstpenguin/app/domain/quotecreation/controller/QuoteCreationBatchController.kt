package com.firstpenguin.app.domain.quotecreation.controller

import com.firstpenguin.app.domain.quotecreation.dto.QuoteCreationBatchStatusResponse
import com.firstpenguin.app.domain.quotecreation.usecase.QuoteCreationBatchUseCase
import com.firstpenguin.app.global.security.ADMIN_BATCH_SECRET_HEADER
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/batches/quote-creation")
@Tag(name = "추천 문장 생성 배치", description = "추천 문장 추출과 후보 검수 배치의 공통 상태 및 결과 동기화 API")
class QuoteCreationBatchController(
    private val quoteCreationBatchUseCase: QuoteCreationBatchUseCase,
) {
    @Operation(
        summary = "추천 문장 생성 배치 처리 현황 조회 API",
        description = "추천 문장 추출/후보 검수 배치의 OpenAI 상태를 동기화한 뒤 처리 현황을 보여준다.",
    )
    @GetMapping("/status")
    fun getQuoteBatchStatus(
        @RequestHeader(ADMIN_BATCH_SECRET_HEADER, required = false) adminSecret: String?,
    ): ResponseEntity<QuoteCreationBatchStatusResponse> =
        ResponseEntity.ok(
            quoteCreationBatchUseCase.getStatus(adminSecret = adminSecret),
        )

    @Operation(
        summary = "추천 문장 생성 배치 결과 동기화 API",
        description = "완료된 추천 문장 추출/후보 검수 배치 결과를 파싱해 추천 문장을 저장하고 상태를 갱신한다.",
    )
    @PostMapping("/{jobId}/sync-result")
    fun syncQuoteBatchResult(
        @PathVariable jobId: Long,
        @RequestHeader(ADMIN_BATCH_SECRET_HEADER, required = false) adminSecret: String?,
    ): ResponseEntity<QuoteCreationBatchStatusResponse> =
        ResponseEntity.ok(
            quoteCreationBatchUseCase.syncBatchResult(
                adminSecret = adminSecret,
                jobId = jobId,
            ),
        )
}
