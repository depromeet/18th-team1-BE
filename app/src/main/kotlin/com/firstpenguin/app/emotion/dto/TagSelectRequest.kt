package com.firstpenguin.app.emotion.dto

data class TagSelectRequest(
    val emotionTagIds: List<Long>,
    val toneTagIds: List<Long>
)