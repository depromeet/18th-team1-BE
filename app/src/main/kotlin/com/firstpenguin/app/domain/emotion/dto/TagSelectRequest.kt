package com.firstpenguin.app.domain.emotion.dto

import jakarta.validation.constraints.NotEmpty

data class TagSelectRequest(
    @field:NotEmpty(message = "감정 태그는 최소 1개 이상 선택해야 합니다.")
    val emotionTagIds: List<Long>,
    @field:NotEmpty(message = "톤 태그는 최소 1개 이상 선택해야 합니다.")
    val toneTagIds: List<Long>,
)
