package com.firstpenguin.app.domain.auth.token

import com.firstpenguin.app.domain.auth.config.AuthProperties
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RefreshTokenCookieManager(
    private val authProperties: AuthProperties,
) {
    fun create(refreshToken: String): ResponseCookie =
        baseCookie(refreshToken)
            .maxAge(authProperties.jwt.refreshTokenExpiration)
            .build()

    fun expire(): ResponseCookie =
        baseCookie("")
            .maxAge(Duration.ZERO)
            .build()

    private fun baseCookie(value: String): ResponseCookie.ResponseCookieBuilder =
        ResponseCookie
            .from(authProperties.refreshToken.cookieName, value)
            .httpOnly(true)
            .secure(authProperties.refreshToken.secure)
            .sameSite("Lax")
            .path(authProperties.refreshToken.cookiePath)
}
