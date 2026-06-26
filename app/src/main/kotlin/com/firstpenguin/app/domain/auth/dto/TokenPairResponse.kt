package com.firstpenguin.app.domain.auth.dto

import com.firstpenguin.app.domain.auth.model.TokenPair
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Access Token과 Refresh Token 발급 응답")
data class TokenPairResponse(
    @field:Schema(
        description = "보호 API 호출 시 Authorization Bearer 헤더에 넣는 Access Token",
        example = "eyJhbGciOiJIUzI1NiJ9...",
    )
    val accessToken: String,
    @field:Schema(
        description = "Access Token 재발급과 로그아웃에 사용할 Refresh Token",
        example = "eyJhbGciOiJIUzI1NiJ9...",
    )
    val refreshToken: String,
) {
    companion object {
        fun from(tokenPair: TokenPair): TokenPairResponse =
            TokenPairResponse(
                accessToken = tokenPair.accessToken,
                refreshToken = tokenPair.refreshToken,
            )
    }
}
