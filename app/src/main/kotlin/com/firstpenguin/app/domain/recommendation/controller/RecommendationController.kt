package com.firstpenguin.app.domain.recommendation.controller

import com.firstpenguin.app.domain.auth.model.AuthenticatedUser
import com.firstpenguin.app.domain.quote.dto.QuoteResponse
import com.firstpenguin.app.domain.recommendation.dto.RecommendationDetailResponse
import com.firstpenguin.app.domain.recommendation.dto.RecommendationPeriodResponse
import com.firstpenguin.app.domain.recommendation.dto.RecommendationRequest
import com.firstpenguin.app.domain.recommendation.dto.RecommendationResponse
import com.firstpenguin.app.domain.recommendation.useCase.RecommendationUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/recommendations")
@Tag(name = "추천", description = "문장 추천 API")
class RecommendationController(
    private val recommendationUseCase: RecommendationUseCase,
) {
    @Operation(
        summary = "추천 시작 API",
        description = "사용자 입력과 선택 태그를 저장한 뒤 최초 추천 문장 1개를 반환한다.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @PostMapping("/quotes")
    fun recommendationQuote(
        @Parameter(hidden = true) @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @Valid @RequestBody request: RecommendationRequest,
    ): ResponseEntity<RecommendationResponse> =
        ResponseEntity.ok(
            recommendationUseCase.recommendQuote(
                userId = authenticatedUser.id,
                request = request,
            ),
        )

    @Operation(
        summary = "추천 문장 더보기 API",
        description = "추천 기록에 추가 후보 문장 9개를 저장하고 반환한다.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @PostMapping("/{recommendationId}/quotes")
    fun getNextRecommendationQuotes(
        @Parameter(hidden = true) @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @PathVariable recommendationId: Long,
    ): ResponseEntity<List<QuoteResponse>> =
        ResponseEntity.ok(
            recommendationUseCase.getNextRecommendationQuotes(
                userId = authenticatedUser.id,
                recommendationId = recommendationId,
            ),
        )

    @Operation(
        summary = "추천 후보 문장 조회 API",
        description = "추천 기록에 저장된 후보 문장 목록을 조회한다. 진행 중인 추천 화면 복구에 사용한다.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @GetMapping("/{recommendationId}/quotes")
    fun getRecommendationQuoteCandidates(
        @Parameter(hidden = true) @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @PathVariable recommendationId: Long,
    ): ResponseEntity<List<QuoteResponse>> =
        ResponseEntity.ok(
            recommendationUseCase.getRecommendationQuoteCandidates(
                userId = authenticatedUser.id,
                recommendationId = recommendationId,
            ),
        )

    @Operation(
        summary = "추천 문장 최종 선택 API",
        description = "추천 후보로 받은 문장 중 하나를 오늘의 문장으로 최종 선택하고 추천 상세 정보를 반환한다.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @PostMapping("/{recommendationId}/quotes/{quoteId}/select")
    fun selectRecommendationQuote(
        @Parameter(hidden = true) @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @PathVariable recommendationId: Long,
        @PathVariable quoteId: Long,
    ): ResponseEntity<RecommendationDetailResponse> =
        ResponseEntity.ok(
            recommendationUseCase.selectRecommendationQuote(
                userId = authenticatedUser.id,
                recommendationId = recommendationId,
                quoteId = quoteId,
            ),
        )

    @Operation(
        summary = "기간별 추천 기록 조회 API",
        description = "기간 내 최종 선택된 추천 기록 목록을 조회한다.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @GetMapping
    fun getRecommendationsByPeriod(
        @Parameter(hidden = true) @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @Parameter(description = "조회 시작일", example = "2026-06-01")
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        start: LocalDate,
        @Parameter(description = "조회 종료일", example = "2026-06-30")
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        end: LocalDate,
    ): ResponseEntity<RecommendationPeriodResponse> =
        ResponseEntity.ok(
            recommendationUseCase.getRecommendationsByPeriod(
                userId = authenticatedUser.id,
                start = start,
                end = end,
            ),
        )

    @Operation(
        summary = "추천 기록 상세 조회 API",
        description = "최종 선택된 추천 기록의 문장, 책, 태그, 사용자 입력을 조회한다.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @GetMapping("/{recommendationId}")
    fun getRecommendationDetail(
        @Parameter(hidden = true) @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @PathVariable recommendationId: Long,
    ): ResponseEntity<RecommendationDetailResponse> =
        ResponseEntity.ok(
            recommendationUseCase.getRecommendationDetail(
                userId = authenticatedUser.id,
                recommendationId = recommendationId,
            ),
        )

    @Operation(
        summary = "추천 기록 삭제 API",
        description = "사용자의 추천 기록, 추천 기록에 연결된 문장, 태그 정보를 삭제한다.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @DeleteMapping("/{recommendationId}")
    fun deleteRecommendation(
        @Parameter(hidden = true) @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @PathVariable recommendationId: Long,
    ): ResponseEntity<Unit> {
        recommendationUseCase.deleteRecommendation(
            userId = authenticatedUser.id,
            recommendationId = recommendationId,
        )

        return ResponseEntity.noContent().build()
    }
}
