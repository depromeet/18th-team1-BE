package com.firstpenguin.app.domain.home.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "홈 요약 응답")
data class HomeSummaryResponse(
    @field:Schema(description = "오늘 작성한 일기. 없으면 null")
    val todayDiary: MonthlyDiaryResponse?,
    @field:Schema(description = "이번 달 일기 목록")
    val monthlyDiaries: List<MonthlyDiaryResponse>,
    @field:Schema(description = "지금까지 작성한 전체 일기 수", example = "42")
    val totalDiaryCount: Int,
)
