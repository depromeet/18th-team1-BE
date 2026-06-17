package com.firstpenguin.app.domain.home.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "홈 요약 응답")
data class HomeSummaryResponse(
    @field:Schema(description = "오늘 최종 선택한 추천 기록 목록")
    val todayRecommendations: List<MonthlyRecommendationResponse>,
    @field:Schema(description = "이번 달 추천 기록 목록")
    val monthlyRecommendations: List<MonthlyRecommendationResponse>,
    @field:Schema(description = "지금까지 최종 선택한 전체 추천 기록 수", example = "42")
    val totalRecommendationCount: Int,
)
