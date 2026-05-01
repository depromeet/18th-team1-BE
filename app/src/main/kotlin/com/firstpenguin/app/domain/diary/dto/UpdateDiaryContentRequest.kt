package com.firstpenguin.app.domain.diary.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "일기 내용 수정 요청")
data class UpdateDiaryContentRequest(
    @field:Size(max = 300, message = "일기 내용은 300자 이하로 입력해주세요")
    @field:Schema(description = "수정할 일기 내용", example = "오늘은 책을 읽고 산책을 했다.")
    val content: String? = null,
)
