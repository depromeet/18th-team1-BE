package com.firstpenguin.app.domain.auth.usecase

import com.firstpenguin.app.domain.auth.dto.AccessTokenResponse
import com.firstpenguin.app.domain.auth.token.JwtTokenProvider
import com.firstpenguin.app.domain.user.model.OAuthUserProfile
import com.firstpenguin.app.domain.user.model.Provider
import com.firstpenguin.app.domain.user.model.User
import com.firstpenguin.app.domain.user.service.UserService
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DevAuthUseCase(
    private val userService: UserService,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    @Transactional
    fun issueDevToken(): AccessTokenResponse {
        val devUser = findOrCreateDevUser()
        return AccessTokenResponse(jwtTokenProvider.createAccessToken(devUser))
    }

    private fun findOrCreateDevUser(): User {
        val user = userService.findOAuthUser(DEV_USER_PROFILE)
        if (user != null) {
            return userService.updateOAuthLogin(user, DEV_USER_PROFILE)
        }

        return userService.createOAuthUser(DEV_USER_PROFILE, DEV_USER_NICKNAME)
            ?: findConcurrentDevUser()
            ?: throw CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS)
    }

    private fun findConcurrentDevUser(): User? {
        val user = userService.findOAuthUser(DEV_USER_PROFILE) ?: return null
        return userService.updateOAuthLogin(user, DEV_USER_PROFILE)
    }

    private companion object {
        const val DEV_USER_NICKNAME = "개발자"
        val DEV_USER_PROFILE =
            OAuthUserProfile(
                provider = Provider.KAKAO,
                providerId = "dev-user",
                email = "dev@firstpenguin.com",
                providerDisplayName = "개발자",
            )
    }
}
