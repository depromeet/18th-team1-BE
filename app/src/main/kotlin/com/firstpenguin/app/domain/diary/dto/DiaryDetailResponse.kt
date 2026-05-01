package com.firstpenguin.app.domain.diary.dto

import com.firstpenguin.app.domain.diary.model.Diary
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "일기 상세 조회 응답")
data class DiaryDetailResponse(
    @field:Schema(description = "일기 ID", example = "1")
    val id: Long,
    @field:Schema(description = "일기 이미지 URL. 연결된 이미지가 없으면 null", example = "https://cdn.example.com/diary.png")
    val diaryImageUrl: String?,
    @field:Schema(description = "책 제목", example = "어린 왕자")
    val title: String,
    @field:Schema(description = "책 저자", example = "앙투안 드 생텍쥐페리")
    val author: String,
    @field:Schema(description = "책 표지 이미지 URL", example = "https://image.aladin.co.kr/product/1/23/cover.jpg")
    val coverImageUrl: String,
    @field:Schema(description = "일기 생성일", example = "2026-05-01")
    val createdAt: LocalDate,
    @field:Schema(description = "일기 내용", example = "오늘은 책을 읽고 산책을 했다.")
    val content: String,
    @field:Schema(description = "감정 온도", example = "HIGH")
    val emotionIntensity: String,
    @field:Schema(description = "문장 내용", example = "가장 중요한 것은 보이지 않는다.")
    val quoteContent: String,
    @field:Schema(description = "알라딘 링크", example = "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=1")
    val aladinLink: String,
) {
    companion object {
        fun from(
            diary: Diary,
            diaryImageUrl: String?,
        ): DiaryDetailResponse =
            DiaryDetailResponse(
                id = diary.id,
                diaryImageUrl = diaryImageUrl,
                title = diary.title,
                author = diary.author,
                coverImageUrl = diary.coverImageUrl,
                createdAt = diary.createdAt.toLocalDate(),
                content = diary.content,
                emotionIntensity = diary.emotionIntensity,
                quoteContent = diary.quoteContent,
                aladinLink = diary.aladinLink,
            )
    }
}
