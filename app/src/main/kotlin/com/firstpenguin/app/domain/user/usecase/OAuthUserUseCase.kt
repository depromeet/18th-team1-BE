package com.firstpenguin.app.domain.user.usecase

import com.firstpenguin.app.domain.user.model.OAuthUserProfile
import com.firstpenguin.app.domain.user.model.User
import com.firstpenguin.app.domain.user.service.UserNicknameGenerator
import com.firstpenguin.app.domain.user.service.UserService
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OAuthUserUseCase(
    private val userNicknameGenerator: UserNicknameGenerator,
    private val userService: UserService,
) {
    @Transactional
    fun upsertOAuthUser(profile: OAuthUserProfile): User {
        repeat(NICKNAME_GENERATION_MAX_ATTEMPT_COUNT) {
            try {
                return userService.upsertOAuthUser(profile, userNicknameGenerator.generate())
            } catch (e: DuplicateKeyException) {
                if (!e.isNicknameConflict()) throw e
            }
        }
        throw CustomException(ErrorCode.NICKNAME_GENERATION_FAILED)
    }

    private fun DuplicateKeyException.isNicknameConflict(): Boolean =
        listOfNotNull(message, rootCause?.message, mostSpecificCause.message)
            .any { it.contains(USER_NICKNAME_UNIQUE_INDEX_NAME) }

    private companion object {
        const val NICKNAME_GENERATION_MAX_ATTEMPT_COUNT = 10
        const val USER_NICKNAME_UNIQUE_INDEX_NAME = "users_nickname_unique_idx"
    }
}
