package com.firstpenguin.app.domain.home.dto

data class TodayStatusResponse(
    val hasOngoingRecommendation: Boolean,
    val ongoingRecommendationId: Long?,
    val canCreateTodayRecommendation: Boolean,
)
