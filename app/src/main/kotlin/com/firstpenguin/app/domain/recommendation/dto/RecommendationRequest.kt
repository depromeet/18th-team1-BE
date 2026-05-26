package com.firstpenguin.app.domain.recommendation.dto

import jakarta.validation.constraints.NotEmpty

data class RecommendationRequest(
    @field:NotEmpty(message = "감정 태그는 최소 1개 이상 선택해야 합니다.")
    val emotionTagIds: List<Long>,
    @field:NotEmpty(message = "기대 태그는 최소 1개 이상 선택해야 합니다.")
    val needTagIds: List<Long>,
    val userContext: String,
)
