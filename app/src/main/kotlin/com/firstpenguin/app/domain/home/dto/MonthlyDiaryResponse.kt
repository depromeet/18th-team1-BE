package com.firstpenguin.app.domain.home.dto

import com.firstpenguin.app.domain.diary.model.Diary
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "이번 달 일기 항목")
data class MonthlyDiaryResponse(
    @field:Schema(description = "일기 ID", example = "23")
    val diaryId: Long,
    @field:Schema(description = "일기 작성일 (서울 시간 기준)", example = "2026-05-06")
    val createdAt: LocalDate,
    @field:Schema(description = "문구 내용", example = "가장 중요한 것은 보이지 않는다.")
    val quoteContent: String,
) {
    companion object {
        fun from(diary: Diary): MonthlyDiaryResponse =
            MonthlyDiaryResponse(
                diaryId = diary.id,
                createdAt = diary.createdAt.toLocalDate(),
                quoteContent = diary.quoteContent,
            )
    }
}
