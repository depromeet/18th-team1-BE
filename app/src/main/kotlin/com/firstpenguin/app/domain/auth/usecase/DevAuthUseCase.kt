package com.firstpenguin.app.domain.auth.usecase

import com.firstpenguin.app.domain.auth.dto.AccessTokenResponse
import com.firstpenguin.app.domain.auth.model.TokenPair
import com.firstpenguin.app.domain.auth.service.RefreshTokenService
import com.firstpenguin.app.domain.auth.token.JwtTokenProvider
import com.firstpenguin.app.domain.user.model.OAuthUserProfile
import com.firstpenguin.app.domain.user.model.Provider
import com.firstpenguin.app.domain.user.model.User
import com.firstpenguin.app.domain.user.service.OAuthUserService
import com.firstpenguin.app.domain.user.service.UserService
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import com.firstpenguin.app.global.security.AdminBatchSecretValidator
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DevAuthUseCase(
    private val oAuthUserService: OAuthUserService,
    private val adminBatchSecretValidator: AdminBatchSecretValidator,
    private val userService: UserService,
    private val refreshTokenService: RefreshTokenService,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    @Transactional
    fun issueDevToken(): AccessTokenResponse {
        val devUser = findOrCreateDevUser()
        return AccessTokenResponse(jwtTokenProvider.createAccessToken(devUser))
    }

    @Transactional
    fun issueTemporaryLoginToken(adminSecret: String?): TokenPair {
        adminBatchSecretValidator.validate(adminSecret)
        val user = userService.getAuthenticatableById(TEMPORARY_LOGIN_USER_ID)
        val refreshToken = refreshTokenService.issue(user)
        return TokenPair(jwtTokenProvider.createAccessToken(user), refreshToken)
    }

    private fun findOrCreateDevUser(): User {
        val user = oAuthUserService.findOAuthUser(DEV_USER_PROFILE)
        if (user != null) {
            return oAuthUserService.updateOAuthLogin(user, DEV_USER_PROFILE)
        }

        return oAuthUserService.createOAuthUser(DEV_USER_PROFILE, DEV_USER_NICKNAME)
            ?: findConcurrentDevUser()
            ?: throw CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS)
    }

    private fun findConcurrentDevUser(): User? {
        val user = oAuthUserService.findOAuthUser(DEV_USER_PROFILE) ?: return null
        return oAuthUserService.updateOAuthLogin(user, DEV_USER_PROFILE)
    }

    private companion object {
        const val DEV_USER_NICKNAME = "개발자"
        const val TEMPORARY_LOGIN_USER_ID = 9065L
        val DEV_USER_PROFILE =
            OAuthUserProfile(
                provider = Provider.KAKAO,
                providerId = "dev-user",
                email = "dev@firstpenguin.com",
                providerDisplayName = "개발자",
            )
    }
}
