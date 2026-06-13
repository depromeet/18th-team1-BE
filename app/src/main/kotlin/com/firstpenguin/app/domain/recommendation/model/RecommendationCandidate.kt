package com.firstpenguin.app.domain.recommendation.model

import com.firstpenguin.app.global.enums.TagType

data class RecommendationCandidate(
    val quoteId: Long,
    val bookId: Long,
    val content: String,
    val title: String,
    val author: String,
    val roleTagId: Long?,
    val tagIdsByType: Map<TagType, Set<Long>>,
)
