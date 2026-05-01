package com.firstpenguin.app.emotion.dto

data class TagSelectResponse(
    val emotionTags: List<TagDto>,
    val toneTags: List<TagDto>
)