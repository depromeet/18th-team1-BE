package com.firstpenguin.app.domain.auth.usecase

import com.firstpenguin.app.domain.auth.dto.AccessTokenResponse
import com.firstpenguin.app.domain.auth.token.JwtTokenProvider
import com.firstpenguin.app.domain.user.model.OAuthUserProfile
import com.firstpenguin.app.domain.user.model.Provider
import com.firstpenguin.app.domain.user.repository.UserRepository
import org.springframework.stereotype.Component

@Component
class DevAuthUseCase(
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    fun issueDevToken(): AccessTokenResponse {
        val devUser = userRepository.upsertOAuthUser(DEV_USER_PROFILE)
        return AccessTokenResponse(jwtTokenProvider.createAccessToken(devUser))
    }

    private companion object {
        val DEV_USER_PROFILE =
            OAuthUserProfile(
                provider = Provider.KAKAO,
                providerId = "dev-user",
                email = "dev@firstpenguin.com",
                nickname = "개발자",
            )
    }
}
