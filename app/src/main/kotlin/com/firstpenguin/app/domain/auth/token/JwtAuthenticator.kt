package com.firstpenguin.app.domain.auth.token

import com.firstpenguin.app.domain.auth.model.AuthenticatedUser
import com.firstpenguin.app.domain.user.service.UserService
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

@Component
class JwtAuthenticator(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userService: UserService,
) {
    fun authenticate(token: String): Authentication {
        val claims = jwtTokenProvider.validateAccessToken(token)
        userService.validateAuthenticatableUser(claims.userId)

        return UsernamePasswordAuthenticationToken(AuthenticatedUser(claims.userId), null, emptyList())
    }
}
