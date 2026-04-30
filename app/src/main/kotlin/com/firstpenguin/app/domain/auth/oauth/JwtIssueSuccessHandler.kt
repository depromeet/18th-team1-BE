package com.firstpenguin.app.domain.auth.oauth

import com.firstpenguin.app.domain.auth.config.AuthProperties
import com.firstpenguin.app.domain.auth.token.RefreshTokenCookieManager
import com.firstpenguin.app.domain.auth.usecase.RefreshTokenUseCase
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class JwtIssueSuccessHandler(
    private val refreshTokenCookieManager: RefreshTokenCookieManager,
    private val refreshTokenUseCase: RefreshTokenUseCase,
    private val authProperties: AuthProperties,
) : AuthenticationSuccessHandler {
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val oAuth2User = authentication.principal as OAuth2AuthenticatedUser
        val refreshToken = refreshTokenUseCase.issue(oAuth2User.user)

        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookieManager.create(refreshToken).toString())
        response.sendRedirect(authProperties.oauth2.successRedirectUrl)
    }
}
