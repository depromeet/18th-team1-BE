package com.firstpenguin.app.domain.diary.model

import java.time.LocalDate

data class DiarySummary(
    val id: Long,
    val createdAt: LocalDate,
    val content: String,
    val emotionIntensity: String,
    val quoteContent: String,
    val coverImageUrl: String,
    val author: String,
    val title: String,
)
