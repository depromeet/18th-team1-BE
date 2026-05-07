package com.firstpenguin.app.domain.diary.dto

import com.firstpenguin.app.domain.diary.model.Diary
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "기간별 일기 조회 응답")
data class DiaryPeriodResponse(
    @field:Schema(description = "조회 시작일", example = "2026-05-01")
    val start: LocalDate,
    @field:Schema(description = "조회 종료일", example = "2026-05-31")
    val end: LocalDate,
    @field:Schema(description = "일기 목록")
    val diaries: List<DiaryResponse>,
) {
    companion object {
        fun from(
            start: LocalDate,
            end: LocalDate,
            diaries: List<Diary>,
        ): DiaryPeriodResponse =
            DiaryPeriodResponse(
                start = start,
                end = end,
                diaries = diaries.map(DiaryResponse::from),
            )
    }
}

@Schema(description = "일기 응답")
data class DiaryResponse(
    @field:Schema(description = "일기 ID", example = "1")
    val id: Long,
    @field:Schema(description = "일기 생성일", example = "2026-05-01")
    val createdAt: LocalDate,
    @field:Schema(description = "일기 내용. 없으면 null", example = "오늘은 책을 읽고 산책을 했다.")
    val content: String?,
    @field:Schema(description = "감정 온도", example = "72")
    val emotionIntensity: Int,
    @field:Schema(description = "문장 내용", example = "가장 중요한 것은 보이지 않는다.")
    val quoteContent: String,
    @field:Schema(description = "책 표지 이미지 URL", example = "https://image.aladin.co.kr/product/1/23/cover.jpg")
    val coverImageUrl: String,
    @field:Schema(description = "저자", example = "앙투안 드 생텍쥐페리")
    val author: String,
    @field:Schema(description = "책 제목", example = "어린 왕자")
    val title: String,
) {
    companion object {
        fun from(diary: Diary): DiaryResponse =
            DiaryResponse(
                id = diary.id,
                createdAt = diary.createdAt.toLocalDate(),
                content = diary.content,
                emotionIntensity = diary.emotionIntensity,
                quoteContent = diary.quoteContent,
                coverImageUrl = diary.coverImageUrl,
                author = diary.author,
                title = diary.title,
            )
    }
}
