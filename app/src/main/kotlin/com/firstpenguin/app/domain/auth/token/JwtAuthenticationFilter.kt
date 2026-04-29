package com.firstpenguin.app.domain.auth.token

import com.firstpenguin.app.domain.auth.model.AuthenticatedUser
import com.firstpenguin.app.global.exception.CustomException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

const val JWT_AUTHENTICATION_ERROR_ATTRIBUTE = "jwtAuthenticationError"

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        authenticate(request, resolveBearerToken(request))
        filterChain.doFilter(request, response)
    }

    private fun authenticate(
        request: HttpServletRequest,
        token: String?,
    ) {
        if (token.isNullOrBlank()) {
            return
        }

        try {
            val claims = jwtTokenProvider.validateAccessToken(token)
            val principal = AuthenticatedUser(id = claims.userId)
            val authentication = UsernamePasswordAuthenticationToken(principal, null, emptyList())
            SecurityContextHolder.getContext().authentication = authentication
        } catch (e: CustomException) {
            log.debug("JWT authentication failed", e)
            request.setAttribute(JWT_AUTHENTICATION_ERROR_ATTRIBUTE, e.errorCode)
            SecurityContextHolder.clearContext()
        }
    }

    private fun resolveBearerToken(request: HttpServletRequest): String? {
        val authorization = request.getHeader(AUTHORIZATION_HEADER)

        if (authorization.isNullOrBlank() || !authorization.startsWith(BEARER_PREFIX)) {
            return null
        }

        return authorization.removePrefix(BEARER_PREFIX).trim()
    }

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val BEARER_PREFIX = "Bearer "
    }
}
