package com.firstpenguin.app.domain.diary.model

import java.time.LocalDateTime

data class Diary(
    val id: Long,
    val userId: Long,
    val quoteId: Long,
    val emotionValue: Int,
    val content: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime?,
    val quoteContent: String,
    val coverImageUrl: String,
    val author: String,
    val title: String,
    val aladinLink: String,
)
