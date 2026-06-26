package com.firstpenguin.app.domain.auth.controller

import com.firstpenguin.app.domain.auth.dto.AccessTokenResponse
import com.firstpenguin.app.domain.auth.dto.TokenPairResponse
import com.firstpenguin.app.domain.auth.token.RefreshTokenCookieManager
import com.firstpenguin.app.domain.auth.usecase.DevAuthUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@ConditionalOnProperty("app.token.enabled", havingValue = "true")
@RequestMapping("/auth")
@Tag(name = "인증")
class DevAuthController(
    private val devAuthUseCase: DevAuthUseCase,
    private val refreshTokenCookieManager: RefreshTokenCookieManager,
) {
    @GetMapping("/dev-token")
    @Operation(
        summary = "[DEV] 개발용 토큰 발급",
        description = "app.token.enabled=true일 때만 활성화. 고정 더미 유저의 Access Token 반환.",
    )
    fun devToken(): AccessTokenResponse = devAuthUseCase.issueDevToken()

    @GetMapping("/temporary-login-token")
    @Operation(
        summary = "[TEMP] 운영 임시 로그인 토큰 발급",
        description = "app.token.enabled=true일 때만 활성화. user_id=9065 사용자의 Access Token과 Refresh Token을 발급합니다.",
    )
    fun temporaryLoginToken(response: HttpServletResponse): TokenPairResponse {
        val tokenPair = devAuthUseCase.issueTemporaryLoginToken()
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookieManager.create(tokenPair.refreshToken).toString())
        return TokenPairResponse.from(tokenPair)
    }
}
