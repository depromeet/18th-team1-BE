package com.firstpenguin.app.domain.emotion.dto

data class TagSelectResponse(
    val emotionTags: List<TagDto>,
    val toneTags: List<TagDto>,
)
