package com.firstpenguin.app.domain.auth.model

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
)
