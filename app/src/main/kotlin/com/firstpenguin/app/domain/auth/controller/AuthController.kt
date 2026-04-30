package com.firstpenguin.app.domain.auth.controller

import com.firstpenguin.app.domain.auth.config.AuthProperties
import com.firstpenguin.app.domain.auth.dto.AccessTokenResponse
import com.firstpenguin.app.domain.auth.token.RefreshTokenCookieManager
import com.firstpenguin.app.domain.auth.usecase.RefreshTokenUseCase
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val refreshTokenCookieManager: RefreshTokenCookieManager,
    private val refreshTokenUseCase: RefreshTokenUseCase,
    private val authProperties: AuthProperties,
) {
    @PostMapping("/refresh")
    fun refresh(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): AccessTokenResponse {
        val refreshToken = request.refreshTokenCookieValue()

        if (refreshToken.isNullOrBlank()) {
            throw CustomException(ErrorCode.REFRESH_TOKEN_REQUIRED)
        }

        val tokenPair = refreshTokenUseCase.rotate(refreshToken)
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookieManager.create(tokenPair.refreshToken).toString())
        return AccessTokenResponse(tokenPair.accessToken)
    }

    @PostMapping("/logout")
    fun logout(request: HttpServletRequest): ResponseEntity<Unit> {
        val refreshToken = request.refreshTokenCookieValue()
        refreshToken?.takeIf { it.isNotBlank() }?.let(refreshTokenUseCase::logout)
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshTokenCookieManager.expire().toString()).build()
    }

    private fun HttpServletRequest.refreshTokenCookieValue(): String? =
        cookies
            ?.firstOrNull { it.name == authProperties.refreshToken.cookieName }
            ?.value
}
