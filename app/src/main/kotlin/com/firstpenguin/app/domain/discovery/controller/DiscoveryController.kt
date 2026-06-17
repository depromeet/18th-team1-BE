package com.firstpenguin.app.domain.discovery.controller

import com.firstpenguin.app.domain.auth.model.AuthenticatedUser
import com.firstpenguin.app.domain.discovery.dto.DiscoveryQuotesResponse
import com.firstpenguin.app.domain.discovery.usecase.DiscoveryUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/discovery")
@Tag(name = "발견탭", description = "추천 이력 기반 발견탭 API")
class DiscoveryController(
    private val discoveryUseCase: DiscoveryUseCase,
) {
    @Operation(
        summary = "발견탭 문장 목록 조회 API",
        description =
            "누군가에게 한 번이라도 추천된 문장을 최신 추천 이력 순으로 10개씩 조회한다. " +
                "첫 페이지는 `cursor` 없이 요청하고, 다음 페이지는 직전 응답의 `nextCursor` 값을 그대로 전달한다. " +
                "`cursor`는 서버가 발급하는 URL-safe Base64 인코딩 문자열이며 클라이언트에서 직접 생성하거나 해석하지 않는다. " +
                "`genre`는 생략하거나 `전체`이면 전체 장르를 조회한다.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @GetMapping("/quotes")
    fun getDiscoveryQuotes(
        @Parameter(hidden = true)
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @Parameter(
            description =
                "다음 페이지 조회 커서. 첫 페이지에서는 생략한다. " +
                    "직전 응답의 `nextCursor` 값을 그대로 전달하며, 클라이언트에서 디코딩하거나 수정하지 않는다.",
            example = "MjA5OS0wNi0wNVQxMjozNDo1NnwxMA",
        )
        @RequestParam(required = false) cursor: String?,
        @Parameter(
            description = "책 장르 필터. 생략하거나 `전체`이면 전체 장르를 조회한다.",
            example = "한국소설",
            schema =
                Schema(
                    allowableValues = [
                        "전체",
                        "한국소설",
                        "일본소설",
                        "영미소설",
                        "판타지",
                        "고전문학",
                        "인문",
                        "철학",
                        "에세이•시",
                        "영화•드라마 원작",
                    ],
                ),
        )
        @RequestParam(required = false) genre: String?,
    ): DiscoveryQuotesResponse =
        discoveryUseCase.getDiscoveryQuotes(
            userId = authenticatedUser.id,
            cursor = cursor,
            genre = genre,
        )

    @Operation(
        summary = "발견탭 문장 검색 API",
        description =
            "추천 이력이 있는 문장을 문장 내용 기준으로 검색한다. " +
                "검색어는 `quotes.content`에만 적용하고, 책 제목/저자/닉네임/감정 값은 검색하지 않는다. " +
                "`sort`는 생략하면 최신순이며, `scrapCount`를 전달하면 스크랩 많은순으로 조회한다. " +
                "`genre`는 생략하거나 `전체`이면 전체 장르를 조회한다. " +
                "다음 페이지는 직전 응답의 `nextCursor` 값을 그대로 전달한다.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @GetMapping("/quotes/search")
    fun searchDiscoveryQuotes(
        @Parameter(hidden = true)
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @Parameter(description = "검색어. 문장 내용에서만 검색한다.", example = "투쟁")
        @RequestParam query: String?,
        @Parameter(
            description = "검색 정렬. 생략하면 최신순이다.",
            example = "latest",
            schema = Schema(allowableValues = ["latest", "scrapCount"]),
        )
        @RequestParam(required = false) sort: String?,
        @Parameter(
            description =
                "다음 페이지 조회 커서. 첫 페이지에서는 생략한다. " +
                    "직전 응답의 `nextCursor` 값을 그대로 전달하며, sort가 바뀌면 재사용하지 않는다.",
            example = "MjA5OS0wNi0wNVQxMjozNDo1NnwxMA",
        )
        @RequestParam(required = false) cursor: String?,
        @Parameter(
            description = "책 장르 필터. 생략하거나 `전체`이면 전체 장르를 조회한다.",
            example = "한국소설",
            schema =
                Schema(
                    allowableValues = [
                        "전체",
                        "한국소설",
                        "일본소설",
                        "영미소설",
                        "판타지",
                        "고전문학",
                        "인문",
                        "철학",
                        "에세이•시",
                        "영화•드라마 원작",
                    ],
                ),
        )
        @RequestParam(required = false) genre: String?,
    ): DiscoveryQuotesResponse =
        discoveryUseCase.searchDiscoveryQuotes(
            userId = authenticatedUser.id,
            query = query,
            sort = sort,
            cursor = cursor,
            genre = genre,
        )
}
