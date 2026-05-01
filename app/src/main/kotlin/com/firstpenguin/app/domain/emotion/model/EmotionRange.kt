package com.firstpenguin.app.domain.emotion.model

import com.firstpenguin.app.global.enums.EmotionRangeName

data class EmotionRange(
    val id: Long,
    val name: EmotionRangeName,
    val minValue: Int,
    val maxValue: Int
)
