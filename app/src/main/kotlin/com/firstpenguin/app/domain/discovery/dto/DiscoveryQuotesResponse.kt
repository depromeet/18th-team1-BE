package com.firstpenguin.app.domain.discovery.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "발견탭 문장 목록 응답")
data class DiscoveryQuotesResponse(
    @field:Schema(description = "발견탭 문장 목록")
    val quotes: List<DiscoveryQuoteResponse>,
    @field:Schema(
        description =
            "다음 페이지 조회 커서. `hasNext`가 true이면 다음 요청의 `cursor`로 그대로 전달한다. " +
                "서버가 발급하는 URL-safe Base64 인코딩 문자열이며 클라이언트에서 직접 생성하거나 해석하지 않는다.",
        example = "MjAyNi0wNi0wNVQxMjozNDo1NnwxMA",
        nullable = true,
    )
    val nextCursor: String?,
    @field:Schema(description = "다음 페이지 존재 여부", example = "true")
    val hasNext: Boolean,
)
