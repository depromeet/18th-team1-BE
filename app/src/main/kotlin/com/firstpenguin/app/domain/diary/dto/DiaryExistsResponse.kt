package com.firstpenguin.app.domain.diary.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "오늘 일기 작성 여부 응답")
data class DiaryExistsResponse(
    @field:Schema(description = "오늘 작성된 일기 존재 여부", example = "true")
    val exists: Boolean,
)
