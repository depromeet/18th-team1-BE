package com.firstpenguin.app.domain.recommendation.dto

import com.firstpenguin.app.domain.emotion.dto.TagDto
import com.firstpenguin.app.domain.quote.dto.QuoteResponse
import java.time.LocalDate

data class RecommendationDetailResponse(
    val recommendationId: Long,
    val quote: QuoteResponse,
    val emotionRangeId: Long,
    val emotionTags: List<TagDto>,
    val needTags: List<TagDto>,
    val feelingText: String?,
    val diaryText: String?,
    val recommendationDate: LocalDate,
)
