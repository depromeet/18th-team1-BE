package com.firstpenguin.app.domain.recommendation.controller

import com.firstpenguin.app.domain.auth.model.AuthenticatedUser
import com.firstpenguin.app.domain.quote.dto.QuoteResponse
import com.firstpenguin.app.domain.recommendation.dto.DailyRecommendationResponse
import com.firstpenguin.app.domain.recommendation.dto.RecommendationRequest
import com.firstpenguin.app.domain.recommendation.dto.RecommendationResponse
import com.firstpenguin.app.domain.recommendation.useCase.RecommendationUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/recommendations")
@Tag(name = "추천", description = "문장 추천 API")
class RecommendationController(
    private val recommendationUseCase: RecommendationUseCase,
) {
    @Operation(
        summary = "일일 추천 상세 조회 API",
        description = "dailyRecommendationId를 기준으로 추천 당시 선택한 태그와 추천 문장 정보를 조회한다.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @GetMapping("/{dailyRecommendationId}")
    fun getDailyRecommendationDetail(
        @Parameter(hidden = true)
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @PathVariable dailyRecommendationId: Long,
    ): ResponseEntity<DailyRecommendationResponse> =
        ResponseEntity.ok(
            recommendationUseCase.getDailyRecommendationDetail(
                userId = authenticatedUser.id,
                dailyRecommendationId = dailyRecommendationId,
            ),
        )

    @Operation(
        summary = "감정/기대 태그 선택, 사용자 문장을 받아 추천된 문장 반환 API",
        description = "사용자가 선택한 감정 태그와 기대 태그를 검증하고, 추천된 문장을 반환한다.",
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
        summary = "문장 더보기 조회 API",
        description = "오늘의 추천 문구 외에 추가 문장 3개를 조회하여 반환한다.",
    )
    @PostMapping("/{dailyRecommendationId}/quotes")
    fun getNextRecommendationQuotes(
        @Parameter(hidden = true) @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @PathVariable dailyRecommendationId: Long,
    ): ResponseEntity<List<QuoteResponse>> =
        ResponseEntity.ok(
            recommendationUseCase.getNextRecommendationQuotes(
                userId = authenticatedUser.id,
                dailyRecommendationId = dailyRecommendationId,
            ),
        )
}
