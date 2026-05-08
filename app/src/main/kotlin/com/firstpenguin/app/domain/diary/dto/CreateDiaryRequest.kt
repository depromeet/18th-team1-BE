package com.firstpenguin.app.domain.diary.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

private const val MIN_EMOTION_VALUE = 1L
private const val MAX_EMOTION_VALUE = 9L

@Schema(description = "일기 생성 요청")
data class CreateDiaryRequest(
    @field:Min(MIN_EMOTION_VALUE)
    @field:Max(MAX_EMOTION_VALUE)
    val emotionValue: Int,
    val tagIds: List<Long>,
    val dailyRecommendationId: Long,
    val quoteId: Long,
    @field:Size(max = 300, message = "일기 내용은 300자 이하로 입력해주세요")
    val content: String? = null,
    val imageIds: List<Long> = emptyList(),
)
