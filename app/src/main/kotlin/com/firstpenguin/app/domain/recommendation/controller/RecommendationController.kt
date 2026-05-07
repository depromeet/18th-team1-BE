package com.firstpenguin.app.domain.recommendation.controller

import com.firstpenguin.app.domain.auth.model.AuthenticatedUser
import com.firstpenguin.app.domain.quote.dto.QuoteResponse
import com.firstpenguin.app.domain.recommendation.dto.RecommendationExistsResponse
import com.firstpenguin.app.domain.recommendation.dto.RecommendationRequest
import com.firstpenguin.app.domain.recommendation.dto.RecommendationResponse
import com.firstpenguin.app.domain.recommendation.useCase.RecommendationUseCase
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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
        summary = "감정/톤 태그 선택, 사용자 문장을 받아 추천된 문장 반환 API",
        description = "사용자가 선택한 감정 태그와 톤 태그를 검증하고, 추천된 문장을 반환한다.",
    )
    @PostMapping("/quotes")
    fun recommendationQuote(
        @Parameter(hidden = true) @AuthenticationPrincipal authenticatedUser: AuthenticatedUser?,
        @Valid @RequestBody request: RecommendationRequest,
    ): ResponseEntity<RecommendationResponse> {
        if (authenticatedUser == null) {
            throw CustomException(ErrorCode.UNAUTHORIZED)
        }

        return ResponseEntity.ok(recommendationUseCase.recommendQuote(authenticatedUser.id, request))
    }

    @Operation(
        summary = "문장 더보기 조회 API",
        description = "오늘의 추천 문구 외에 추가 문장 3개를 조회하여 반환한다.",
    )
    @PostMapping("/{dailyRecommendationId}/quotes")
    fun getNextRecommendationQuotes(
        @Parameter(hidden = true) @AuthenticationPrincipal authenticatedUser: AuthenticatedUser?,
        @PathVariable dailyRecommendationId: Long,
    ): ResponseEntity<List<QuoteResponse>> {
        if (authenticatedUser == null) {
            throw CustomException(ErrorCode.UNAUTHORIZED)
        }

        return ResponseEntity.ok(
            recommendationUseCase.getNextRecommendationQuotes(
                userId = authenticatedUser.id,
                dailyRecommendationId = dailyRecommendationId,
            ),
        )
    }

    @Operation(
        summary = "오늘 추천 문구 생성 여부 조회 API",
        description = "로그인한 사용자가 오늘 생성한 추천 문구가 있는지 조회한다.",
    )
    @GetMapping("/today/exists")
    fun hasTodayRecommendation(
        @Parameter(hidden = true) @AuthenticationPrincipal authenticatedUser: AuthenticatedUser?,
    ): ResponseEntity<RecommendationExistsResponse> {
        if (authenticatedUser == null) {
            throw CustomException(ErrorCode.UNAUTHORIZED)
        }

        return ResponseEntity.ok(recommendationUseCase.hasTodayRecommendation(authenticatedUser.id))
    }
}
