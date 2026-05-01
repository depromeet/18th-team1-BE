package com.firstpenguin.app.domain.book.model

import java.time.LocalDateTime

data class Book(
    val id: Long,
    val title: String,
    val author: String,
    val isbn13: String,
    val aladinLink: String,
    val coverImageUrl: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime?,
)
