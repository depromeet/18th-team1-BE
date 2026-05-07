package com.firstpenguin.app.domain.recommendation.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "오늘 추천 문구 생성 여부 응답")
data class RecommendationExistsResponse(
    @field:Schema(description = "오늘 생성된 추천 문구 존재 여부", example = "true")
    val exists: Boolean,
)
