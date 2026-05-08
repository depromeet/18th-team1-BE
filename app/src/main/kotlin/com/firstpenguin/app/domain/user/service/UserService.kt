package com.firstpenguin.app.domain.user.service

import com.firstpenguin.app.domain.user.model.OAuthUserProfile
import com.firstpenguin.app.domain.user.model.User
import com.firstpenguin.app.domain.user.model.UserStatus
import com.firstpenguin.app.domain.user.repository.UserRepository
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
) {
    fun getById(id: Long): User = userRepository.findById(id) ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

    fun validateAuthenticatableUser(userId: Long) {
        val user = userRepository.findById(userId) ?: throw CustomException(ErrorCode.AUTH_USER_NOT_FOUND)
        validateAuthenticatableStatus(user)
    }

    fun upsertOAuthUser(profile: OAuthUserProfile): User = userRepository.upsertOAuthUser(profile)

    fun updateProfile(
        userId: Long,
        nickname: String?,
        profileImageId: Long?,
    ) {
        if (nickname != null && nickname.isBlank()) throw CustomException(ErrorCode.INVALID_INPUT)
        userRepository.update(userId, nickname, profileImageId)
    }

    private fun validateAuthenticatableStatus(user: User) {
        val errorCode =
            when {
                user.deletedAt != null -> ErrorCode.AUTH_USER_DELETED
                else -> authenticatableStatusErrorCode(user.status)
            }

        errorCode?.let { throw CustomException(it) }
    }

    private fun authenticatableStatusErrorCode(status: UserStatus): ErrorCode? =
        when (status) {
            UserStatus.ACTIVE -> null
            UserStatus.BLOCKED -> ErrorCode.AUTH_USER_BLOCKED
            UserStatus.DELETED -> ErrorCode.AUTH_USER_DELETED
        }
}
