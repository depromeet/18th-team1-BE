package com.firstpenguin.app.domain.discovery.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "발견탭 문장 목록 응답")
data class DiscoveryQuotesResponse(
    @field:Schema(description = "랜덤 발견탭 문장 목록")
    val quotes: List<DiscoveryQuoteResponse>,
)
