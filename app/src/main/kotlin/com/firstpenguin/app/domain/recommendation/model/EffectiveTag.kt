package com.firstpenguin.app.domain.recommendation.model

import com.firstpenguin.app.global.enums.TagType

data class EffectiveTag(
    val tagId: Long,
    val type: TagType,
    val weight: Double = 1.0,
)
