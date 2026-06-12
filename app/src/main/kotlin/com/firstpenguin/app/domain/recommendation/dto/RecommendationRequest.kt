package com.firstpenguin.app.domain.recommendation.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

private const val MIN_EMOTION_VALUE = 1L
private const val MAX_EMOTION_VALUE = 9L

data class RecommendationRequest(
    @field:Min(MIN_EMOTION_VALUE)
    @field:Max(MAX_EMOTION_VALUE)
    val emotionRangeId: Long,
    @field:NotEmpty(message = "감정 태그는 최소 1개 이상 선택해야 합니다.")
    @field:Size(max = 5, message = "감정 태그는 최대 5개까지 선택할 수 있습니다.")
    val emotionTagIds: List<Long>,
    val needTagIds: List<Long> = emptyList(),
    val feelingText: String?,
    val diaryText: String?,
)
