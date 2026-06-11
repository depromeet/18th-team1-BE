package com.firstpenguin.app.domain.discovery.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "발견탭 문장 목록 응답")
data class DiscoveryQuotesResponse(
    @field:Schema(description = "발견탭 문장 목록")
    val quotes: List<DiscoveryQuoteResponse>,
    @field:Schema(
        description = "다음 페이지 조회 커서. `hasNext`가 true이면 다음 요청의 `cursor`로 그대로 전달한다.",
        example = "MjAyNi0wNi0wNVQxMjozNDo1NnwxMA",
        nullable = true,
    )
    val nextCursor: String?,
    @field:Schema(description = "다음 페이지 존재 여부", example = "true")
    val hasNext: Boolean,
)
