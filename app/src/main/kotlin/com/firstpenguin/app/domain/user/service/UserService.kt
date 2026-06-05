package com.firstpenguin.app.domain.user.service

import com.firstpenguin.app.domain.user.model.OAuthUserProfile
import com.firstpenguin.app.domain.user.model.User
import com.firstpenguin.app.domain.user.model.UserStatus
import com.firstpenguin.app.domain.user.repository.UserRepository
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.dao.DuplicateKeyException
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

    fun findOAuthUser(profile: OAuthUserProfile): User? =
        userRepository.findByProviderAndProviderId(
            profile.provider,
            profile.providerId,
        )

    fun createOAuthUser(
        profile: OAuthUserProfile,
        nickname: String,
    ): User? = userRepository.createOAuthUser(profile, nickname)

    fun updateOAuthLogin(
        user: User,
        profile: OAuthUserProfile,
    ): User {
        validateOAuthUserMatchesProfile(user, profile)
        validateAuthenticatableStatus(user)
        return updateOAuthLogin(profile)
    }

    private fun updateOAuthLogin(profile: OAuthUserProfile): User =
        userRepository.updateOAuthLogin(profile) ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

    fun updateProfile(
        userId: Long,
        nickname: String?,
        profileImageId: Long?,
    ) {
        nickname?.let { validateNicknameAvailable(userId, it) }

        try {
            userRepository.update(userId, nickname, profileImageId)
        } catch (e: DuplicateKeyException) {
            if (nickname == null) throw e
            throw CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS).also { it.initCause(e) }
        }
    }

    private fun validateNicknameAvailable(
        userId: Long,
        nickname: String,
    ) {
        if (nickname.isBlank() || nickname in RESERVED_NICKNAMES) throw CustomException(ErrorCode.INVALID_INPUT)
        if (userRepository.existsByNickname(nickname, userId)) throw CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS)
    }

    private fun validateOAuthUserMatchesProfile(
        user: User,
        profile: OAuthUserProfile,
    ) {
        if (user.provider == profile.provider && user.providerId == profile.providerId) return
        throw CustomException(ErrorCode.INTERNAL_SERVER_ERROR)
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

    private companion object {
        val RESERVED_NICKNAMES = setOf("개발자")
    }
}
