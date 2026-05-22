package com.firstpenguin.app.domain.emotion.dto

import com.firstpenguin.app.domain.emotion.model.Tag
import com.firstpenguin.app.global.enums.TagType

data class TagDto(
    val id: Long,
    val label: String,
    val type: TagType,
    val emotionRangeId: Long?,
) {
    companion object {
        fun from(tag: Tag): TagDto =
            TagDto(
                id = tag.id,
                label = tag.label,
                type = tag.type,
                emotionRangeId = tag.emotionRangeId,
            )
    }
}
