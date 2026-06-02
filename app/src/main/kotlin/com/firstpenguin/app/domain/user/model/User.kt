package com.firstpenguin.app.domain.user.model

import java.time.LocalDateTime

data class User(
    val id: Long,
    val provider: Provider,
    val providerId: String,
    val email: String?,
    val providerDisplayName: String?,
    val nickname: String,
    val profileImageId: Long?,
    val status: UserStatus,
    val lastLoginAt: LocalDateTime?,
    val deletedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
