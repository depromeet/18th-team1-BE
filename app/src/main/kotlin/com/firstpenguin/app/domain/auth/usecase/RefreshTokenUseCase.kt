package com.firstpenguin.app.domain.auth.usecase

import com.firstpenguin.app.domain.auth.model.TokenPair
import com.firstpenguin.app.domain.auth.service.RefreshTokenService
import com.firstpenguin.app.domain.user.model.User
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RefreshTokenUseCase(
    private val refreshTokenService: RefreshTokenService,
) {
    @Transactional
    fun issue(user: User): String = refreshTokenService.issue(user)

    @Transactional
    fun rotate(refreshToken: String): TokenPair = refreshTokenService.rotate(refreshToken)

    @Transactional
    fun logout(refreshToken: String) {
        refreshTokenService.logout(refreshToken)
    }
}
