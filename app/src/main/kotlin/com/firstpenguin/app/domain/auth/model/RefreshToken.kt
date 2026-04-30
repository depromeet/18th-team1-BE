package com.firstpenguin.app.domain.auth.model

import java.time.LocalDateTime

data class RefreshToken(
    val id: Long,
    val userId: Long,
    val deviceId: String,
    val tokenHash: String,
    val expiresAt: LocalDateTime,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
