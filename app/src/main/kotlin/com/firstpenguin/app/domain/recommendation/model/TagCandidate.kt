package com.firstpenguin.app.domain.recommendation.model

import com.firstpenguin.app.global.enums.TagType

data class TagCandidate(
    val tagId: Long,
    val code: String,
    val label: String,
    val type: TagType,
    val source: TagCandidateSource,
    val priority: TagCandidatePriority,
    val confidence: Double,
)
