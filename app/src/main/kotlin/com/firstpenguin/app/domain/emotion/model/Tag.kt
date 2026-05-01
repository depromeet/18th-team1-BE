package com.firstpenguin.app.domain.emotion.model

import com.firstpenguin.app.global.enums.TagType
import java.time.LocalDateTime

data class Tag(
    val id: Long,
    val emotionRangeId: Long?,
    val label: String,
    val type: TagType,
    val createdAt: LocalDateTime,
)
