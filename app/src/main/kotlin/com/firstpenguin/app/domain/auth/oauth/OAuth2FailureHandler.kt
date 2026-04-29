package com.firstpenguin.app.domain.auth.oauth

import com.firstpenguin.app.domain.auth.config.AuthProperties
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

@Component
class OAuth2FailureHandler(
    private val authProperties: AuthProperties,
) : AuthenticationFailureHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException,
    ) {
        log.warn("OAuth2 login failed. uri={}", request.requestURI, exception)
        response.sendRedirect(failureRedirectUrl())
    }

    private fun failureRedirectUrl(): String =
        UriComponentsBuilder
            .fromUriString(authProperties.oauth2.failureRedirectUrl)
            .queryParam("error", "oauth2_login_failed")
            .build(true)
            .toUriString()
}
