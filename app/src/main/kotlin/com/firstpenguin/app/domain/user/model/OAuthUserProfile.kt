package com.firstpenguin.app.domain.user.model

data class OAuthUserProfile(
    val provider: Provider,
    val providerId: String,
    val email: String?,
    val nickname: String,
)
