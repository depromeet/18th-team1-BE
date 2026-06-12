package com.firstpenguin.app.domain.recommendation.model

import com.firstpenguin.app.global.enums.TagType

data class TagCandidate(
    val tagId: Long,
    val type: TagType,
    val source: TagCandidateSource,
    val priority: TagCandidatePriority,
    val confidence: Double,
    val label: String? = null,
    val reason: String? = null,
)
