package com.firstpenguin.app.domain.diary.model

import java.time.LocalDateTime

data class Diary(
    val id: Long,
    val userId: Long,
    val emotionIntensity: String,
    val content: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime?,
)
