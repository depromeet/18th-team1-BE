package com.firstpenguin.app.domain.emotion.dto

data class TagSelectRequest(
    val emotionTagIds: List<Long>,
    val toneTagIds: List<Long>
)
