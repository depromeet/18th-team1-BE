package com.firstpenguin.app.domain.recommendation.model

import com.firstpenguin.app.global.enums.TagType

data class EffectiveTag(
    val tagId: Long,
    val code: String,
    val type: TagType,
    val importance: Double = 1.0,
)
