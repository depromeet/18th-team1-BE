package com.firstpenguin.app.domain.quote.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "마이페이지 스크랩 문장 목록 응답")
data class ScrappedQuotesResponse(
    @field:Schema(description = "로그인 사용자가 스크랩한 문장 총 개수", example = "37")
    val totalCount: Int,
    @field:Schema(description = "현재 페이지의 스크랩 문장 목록")
    val quotes: List<ScrappedQuoteResponse>,
    @field:Schema(
        description = "다음 페이지 조회 커서. 다음 페이지가 없으면 null",
        example = "MjAyNi0wNi0xM1QxNDozMDowMHwx",
        nullable = true,
    )
    val nextCursor: String?,
    @field:Schema(description = "다음 페이지 존재 여부", example = "true")
    val hasNext: Boolean,
)
