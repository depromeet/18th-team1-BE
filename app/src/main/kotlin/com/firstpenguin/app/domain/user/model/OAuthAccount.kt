package com.firstpenguin.app.domain.user.model

import java.time.LocalDateTime

data class OAuthAccount(
    val id: Long,
    val userId: Long,
    val provider: Provider,
    val providerId: String,
    val email: String?,
    val providerDisplayName: String?,
    val lastLoginAt: LocalDateTime?,
    val disconnectedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
