package com.firstpenguin.app.domain.user.model

import java.time.LocalDateTime

data class User(
    val id: Long,
    val nickname: String,
    val profileImageId: Long?,
    val status: UserStatus,
    val withdrawalRequestedAt: LocalDateTime?,
    val withdrawalDueAt: LocalDateTime?,
    val deletedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
