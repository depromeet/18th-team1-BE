package com.firstpenguin.app.domain.auth.controller

import com.firstpenguin.app.domain.auth.dto.AccessTokenResponse
import com.firstpenguin.app.domain.auth.service.RefreshTokenService
import com.firstpenguin.app.domain.auth.token.RefreshTokenCookieManager
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val refreshTokenService: RefreshTokenService,
    private val refreshTokenCookieManager: RefreshTokenCookieManager,
) {
    @PostMapping("/refresh")
    fun refresh(
        @CookieValue(name = "refresh_token", required = false) refreshToken: String?,
        response: HttpServletResponse,
    ): AccessTokenResponse {
        if (refreshToken.isNullOrBlank()) {
            throw CustomException(ErrorCode.REFRESH_TOKEN_REQUIRED)
        }

        val tokenPair = refreshTokenService.rotate(refreshToken)
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookieManager.create(tokenPair.refreshToken).toString())
        return AccessTokenResponse(tokenPair.accessToken)
    }

    @PostMapping("/logout")
    fun logout(
        @CookieValue(name = "refresh_token", required = false) refreshToken: String?,
    ): ResponseEntity<Unit> {
        refreshToken?.takeIf { it.isNotBlank() }?.let(refreshTokenService::logout)
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshTokenCookieManager.expire().toString()).build()
    }
}
