package com.firstpenguin.app.domain.diary.model

import java.time.LocalDateTime

data class Diary(
    val id: Long,
    val userId: Long,
    val quoteId: Long,
    val diaryImageId: Long?,
    val emotionIntensity: String,
    val content: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime?,
    val quoteContent: String? = null,
    val coverImageUrl: String? = null,
    val author: String? = null,
    val title: String? = null,
    val aladinLink: String? = null,
)
