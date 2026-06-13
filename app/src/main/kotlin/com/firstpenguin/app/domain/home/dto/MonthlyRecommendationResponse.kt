package com.firstpenguin.app.domain.home.dto

import com.firstpenguin.app.domain.quote.model.Quote
import com.firstpenguin.app.domain.recommendation.model.Recommendation
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "이번 달 추천 기록 항목")
data class MonthlyRecommendationResponse(
    @field:Schema(description = "추천 기록 ID", example = "23")
    val recommendationId: Long,
    @field:Schema(description = "추천 생성일 (서울 시간 기준)", example = "2026-05-06")
    val createdAt: LocalDate,
    @field:Schema(description = "문구 내용", example = "가장 중요한 것은 보이지 않는다.")
    val quoteContent: String,
) {
    companion object {
        fun from(
            recommendation: Recommendation,
            quote: Quote,
        ): MonthlyRecommendationResponse =
            MonthlyRecommendationResponse(
                recommendationId = recommendation.id,
                createdAt = recommendation.createdAt.toLocalDate(),
                quoteContent = quote.content,
            )
    }
}
