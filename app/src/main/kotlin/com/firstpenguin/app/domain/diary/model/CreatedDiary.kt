package com.firstpenguin.app.domain.diary.model

import java.time.LocalDateTime

data class CreatedDiary(
    val diaryId: Long,
    val createdAt: LocalDateTime,
)
