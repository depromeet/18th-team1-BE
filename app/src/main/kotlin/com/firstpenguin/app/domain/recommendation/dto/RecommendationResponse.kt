package com.firstpenguin.app.domain.recommendation.dto

import com.firstpenguin.app.domain.quote.dto.QuoteResponse

data class RecommendationResponse(
    val recommendationId: Long,
    val quote: QuoteResponse,
)
