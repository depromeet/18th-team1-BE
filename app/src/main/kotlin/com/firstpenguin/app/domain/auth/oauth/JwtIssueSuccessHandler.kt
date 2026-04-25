package com.firstpenguin.app.domain.auth.oauth

import com.firstpenguin.app.domain.auth.config.AuthProperties
import com.firstpenguin.app.domain.auth.service.RefreshTokenService
import com.firstpenguin.app.domain.auth.token.JwtTokenProvider
import com.firstpenguin.app.domain.auth.token.RefreshTokenCookieManager
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

@Component
class JwtIssueSuccessHandler(
    private val jwtTokenProvider: JwtTokenProvider,
    private val refreshTokenService: RefreshTokenService,
    private val refreshTokenCookieManager: RefreshTokenCookieManager,
    private val authProperties: AuthProperties,
) : AuthenticationSuccessHandler {
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val oAuth2User = authentication.principal as OAuth2AuthenticatedUser
        val accessToken = jwtTokenProvider.createAccessToken(oAuth2User.user)
        val refreshToken = refreshTokenService.issue(oAuth2User.user)

        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookieManager.create(refreshToken).toString())
        response.sendRedirect(successRedirectUrl(accessToken))
    }

    private fun successRedirectUrl(accessToken: String): String =
        UriComponentsBuilder
            .fromUriString(authProperties.oauth2.successRedirectUrl)
            .fragment("accessToken=$accessToken")
            .build(true)
            .toUriString()
}
