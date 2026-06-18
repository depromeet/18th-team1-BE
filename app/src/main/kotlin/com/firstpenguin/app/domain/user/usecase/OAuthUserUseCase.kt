package com.firstpenguin.app.domain.user.usecase

import com.firstpenguin.app.domain.user.model.OAuthUserProfile
import com.firstpenguin.app.domain.user.model.User
import com.firstpenguin.app.domain.user.service.OAuthUserService
import com.firstpenguin.app.domain.user.service.UserNicknameGenerator
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OAuthUserUseCase(
    private val userNicknameGenerator: UserNicknameGenerator,
    private val oAuthUserService: OAuthUserService,
) {
    @Transactional
    fun loginOAuthUser(profile: OAuthUserProfile): User {
        val user = oAuthUserService.findOAuthUser(profile)
        if (user != null) {
            return oAuthUserService.updateOAuthLogin(user, profile)
        }

        return createOAuthUser(profile)
    }

    private fun createOAuthUser(profile: OAuthUserProfile): User {
        repeat(NICKNAME_GENERATION_MAX_ATTEMPT_COUNT) {
            oAuthUserService.createOAuthUser(profile, userNicknameGenerator.generate())?.let { return it }
            val user = oAuthUserService.findOAuthUser(profile)
            if (user != null) return oAuthUserService.updateOAuthLogin(user, profile)
        }
        throw CustomException(ErrorCode.NICKNAME_GENERATION_FAILED)
    }

    private companion object {
        const val NICKNAME_GENERATION_MAX_ATTEMPT_COUNT = 10
    }
}
