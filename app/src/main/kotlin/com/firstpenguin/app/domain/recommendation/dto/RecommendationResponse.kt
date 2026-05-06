package com.firstpenguin.app.domain.recommendation.dto

import com.firstpenguin.app.domain.emotion.dto.TagDto
import com.firstpenguin.app.domain.quote.dto.QuoteResponse

data class RecommendationResponse(
    val dailyRecommendationId: Long,
    val quote: QuoteResponse,
    val emotionTags: List<TagDto>,
    val toneTags: List<TagDto>,
)
