package com.firstpenguin.app.domain.emotion.dto

import com.firstpenguin.app.global.enums.TagType

data class TagDto(
    val id: Long,
    val label: String,
    val type: TagType,
)