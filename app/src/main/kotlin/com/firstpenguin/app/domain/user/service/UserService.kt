package com.firstpenguin.app.domain.user.service

import com.firstpenguin.app.domain.user.model.User
import com.firstpenguin.app.domain.user.model.UserStatus
import com.firstpenguin.app.domain.user.repository.UserRepository
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class UserService(
    private val userRepository: UserRepository,
) {
    fun getById(id: Long): User = userRepository.findById(id) ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

    fun getAuthenticatableById(id: Long): User {
        val user = userRepository.findById(id) ?: throw CustomException(ErrorCode.AUTH_USER_NOT_FOUND)
        validateAuthenticatableStatus(user)
        return user
    }

    fun validateAuthenticatableUser(userId: Long) {
        val user = userRepository.findById(userId) ?: throw CustomException(ErrorCode.AUTH_USER_NOT_FOUND)
        validateAuthenticatableStatus(user)
    }

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

    fun requestWithdrawal(userId: Long) {
        val user = getById(userId)
        validateAuthenticatableStatus(user)

        val now = LocalDateTime.now()
        val updatedCount = userRepository.requestWithdrawal(user.id, now, now.plusDays(WITHDRAWAL_GRACE_DAYS))
        if (updatedCount == 0) throw CustomException(ErrorCode.USER_NOT_FOUND)
    }

    private fun validateNicknameAvailable(
        userId: Long,
        nickname: String,
    ) {
        if (nickname.isBlank() || nickname in RESERVED_NICKNAMES) throw CustomException(ErrorCode.INVALID_INPUT)
        if (userRepository.existsByNickname(nickname, userId)) throw CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS)
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
            UserStatus.WITHDRAWAL_REQUESTED -> ErrorCode.AUTH_USER_DELETED
            UserStatus.DELETED -> ErrorCode.AUTH_USER_DELETED
        }

    private companion object {
        const val WITHDRAWAL_GRACE_DAYS = 30L
        val RESERVED_NICKNAMES = setOf("개발자")
    }
}
