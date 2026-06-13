package com.firstpenguin.app.domain.recommendation.dto

import com.firstpenguin.app.domain.quote.dto.QuoteResponse
import com.firstpenguin.app.domain.recommendation.model.Recommendation
import java.time.LocalDate

data class RecommendationPeriodResponse(
    val start: LocalDate,
    val end: LocalDate,
    val recommendations: List<RecommendationPeriodItemResponse>,
) {
    companion object {
        fun from(
            start: LocalDate,
            end: LocalDate,
            recommendations: List<RecommendationPeriodItemResponse>,
        ): RecommendationPeriodResponse =
            RecommendationPeriodResponse(
                start = start,
                end = end,
                recommendations = recommendations,
            )
    }
}

data class RecommendationPeriodItemResponse(
    val recommendationId: Long,
    val recommendationDate: LocalDate,
    val quote: QuoteResponse,
) {
    companion object {
        fun from(
            recommendation: Recommendation,
            quote: QuoteResponse,
        ): RecommendationPeriodItemResponse =
            RecommendationPeriodItemResponse(
                recommendationId = recommendation.id,
                recommendationDate = recommendation.recommendationDate,
                quote = quote,
            )
    }
}
