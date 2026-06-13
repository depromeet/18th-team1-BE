package com.firstpenguin.app.domain.recommendation.model

import java.time.LocalDateTime

data class RecommendationTag(
    val id: Long,
    val recommendationId: Long,
    val tagId: Long,
    val createdAt: LocalDateTime,
)
