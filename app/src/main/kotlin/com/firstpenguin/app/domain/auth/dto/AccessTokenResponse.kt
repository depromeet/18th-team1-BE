package com.firstpenguin.app.domain.auth.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Access Token 재발급 응답")
data class AccessTokenResponse(
    @field:Schema(
        description = "보호 API 호출 시 Authorization Bearer 헤더에 넣는 Access Token",
        example = "eyJhbGciOiJIUzI1NiJ9...",
    )
    val accessToken: String,
)
