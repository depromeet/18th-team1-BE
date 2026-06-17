package com.firstpenguin.app.domain.discovery.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.firstpenguin.app.domain.discovery.model.DiscoveryNeedTag
import com.firstpenguin.app.domain.discovery.model.DiscoveryQuote
import com.firstpenguin.app.domain.emotion.model.EmotionLevel
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "발견탭 문장 응답")
data class DiscoveryQuoteResponse(
    @field:Schema(description = "문장 ID", example = "1")
    val quoteId: Long,
    @field:Schema(description = "책 ID", example = "1")
    val bookId: Long,
    @field:Schema(description = "이 문장을 추천받은 사용자 ID", example = "1")
    val recommendedUserId: Long,
    @field:Schema(description = "문장 내용", example = "새는 알에서 나오려고 투쟁한다.")
    val content: String,
    @field:Schema(description = "책 제목", example = "데미안")
    val title: String,
    @field:Schema(description = "저자", example = "헤르만 헤세")
    val author: String,
    @field:Schema(description = "책 표지 이미지 URL", example = "https://cdn.example.com/book-cover-placeholder.png")
    val bookCoverImageUrl: String,
    @field:Schema(description = "책 장르", example = "한국소설", nullable = true)
    val genre: String?,
    @field:Schema(description = "추천 당시 선택한 NEED 태그. 선택 태그가 없으면 null", nullable = true)
    val needTag: DiscoveryNeedTagResponse?,
    @field:Schema(
        description =
            "추천 당시 입력한 감정 단계. " +
                "1=아주 별로에요, 2=별로에요, 3=약간 별로에요, 4=그저그래요, 5=나쁘지 않아요, " +
                "6=꽤 괜찮아요, 7=약간 기분 좋아요, 8=기분 좋아요, 9=아주 기분 좋아요!",
        example = "7",
        allowableValues = ["1", "2", "3", "4", "5", "6", "7", "8", "9"],
    )
    val emotionValue: Int,
    @field:Schema(description = "추천 당시 입력한 감정 단계 표시 문구", example = "약간 기분 좋아요")
    val emotionLabel: String,
    @field:Schema(description = "문장이 추천 이력에 등록된 시각", example = "2026-06-05T12:34:56")
    val recommendedAt: LocalDateTime,
    @get:JsonProperty("isScrapped")
    @field:Schema(description = "로그인 사용자의 스크랩 여부", example = "false")
    val isScrapped: Boolean,
) {
    companion object {
        fun from(quote: DiscoveryQuote): DiscoveryQuoteResponse =
            DiscoveryQuoteResponse(
                quoteId = quote.quoteId,
                bookId = quote.bookId,
                recommendedUserId = quote.recommendedUserId,
                content = quote.content,
                title = quote.title,
                author = quote.author,
                bookCoverImageUrl = quote.bookCoverImageUrl,
                genre = quote.genre,
                needTag = quote.needTag?.let(DiscoveryNeedTagResponse::from),
                emotionValue = quote.emotionValue,
                emotionLabel = EmotionLevel.from(quote.emotionValue).label,
                recommendedAt = quote.recommendedAt,
                isScrapped = quote.isScrapped,
            )
    }
}

@Schema(description = "추천 당시 선택한 NEED 태그 응답")
data class DiscoveryNeedTagResponse(
    @field:Schema(description = "NEED 태그 ID", example = "49")
    val id: Long,
    @field:Schema(description = "NEED 태그 라벨", example = "공감해주는 문장")
    val label: String,
) {
    companion object {
        fun from(needTag: DiscoveryNeedTag): DiscoveryNeedTagResponse =
            DiscoveryNeedTagResponse(
                id = needTag.id,
                label = needTag.label,
            )
    }
}
