package com.firstpenguin.app.domain.batch.dto

import com.firstpenguin.app.global.enums.TagType

data class TagOption(
    val id: Long,
    val type: TagType,
    val code: String,
    val label: String,
)
