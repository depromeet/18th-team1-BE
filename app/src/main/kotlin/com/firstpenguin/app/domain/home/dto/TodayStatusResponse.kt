package com.firstpenguin.app.domain.home.dto

data class TodayStatusResponse(
    val hasTodayRecommendation: Boolean,
    val hasTodayDiary: Boolean,
    val dailyRecommendationId: Long?,
)
