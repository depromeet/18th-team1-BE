package com.firstpenguin.app.domain.diary.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "일기 생성 응답")
data class CreateDiaryResponse(
    val diaryId: Long,
    val createdAt: LocalDateTime,
)
