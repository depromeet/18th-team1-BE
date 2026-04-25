package com.firstpenguin.app.domain.auth.model

import com.firstpenguin.app.domain.user.model.Role

data class TokenClaims(
    val userId: Long,
    val role: Role?,
)
