package com.firstpenguin.app.domain.home.controller

import com.firstpenguin.app.domain.auth.model.AuthenticatedUser
import com.firstpenguin.app.domain.home.dto.HomeSummaryResponse
import com.firstpenguin.app.domain.home.userCase.HomeUserCase
import com.firstpenguin.app.domain.quote.dto.QuoteResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/home")
@Tag(name = "HOME", description = "홈 API")
class HomeController(
    private val homeUserCase: HomeUserCase,
) {
    @Operation(
        summary = "랜덤 추천 문구 API",
        description = "홈화면 랜덤 추천 문구를 정보를 반환한다.",
    )
    @GetMapping("/random")
    fun getRandomQuote(): ResponseEntity<QuoteResponse> = ResponseEntity.ok(homeUserCase.getRandomQuote())

    @Operation(
        summary = "홈 요약 API",
        description = "오늘 작성한 일기, 이번 달 일기 목록, 전체 일기 수를 반환한다.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "홈 요약 조회 성공",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = HomeSummaryResponse::class),
                    ),
                ],
            ),
        ],
    )
    @GetMapping("/summary")
    fun getSummary(
        @Parameter(hidden = true)
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<HomeSummaryResponse> = ResponseEntity.ok(homeUserCase.getSummary(authenticatedUser.id))
}
