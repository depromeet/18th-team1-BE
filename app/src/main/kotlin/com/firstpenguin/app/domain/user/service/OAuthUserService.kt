package com.firstpenguin.app.domain.user.service

import com.firstpenguin.app.domain.user.model.OAuthAccount
import com.firstpenguin.app.domain.user.model.OAuthUserProfile
import com.firstpenguin.app.domain.user.model.User
import com.firstpenguin.app.domain.user.model.UserStatus
import com.firstpenguin.app.domain.user.repository.OAuthAccountRepository
import com.firstpenguin.app.domain.user.repository.UserRepository
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class OAuthUserService(
    private val oAuthAccountRepository: OAuthAccountRepository,
    private val userRepository: UserRepository,
) {
    fun findOAuthUser(profile: OAuthUserProfile): User? =
        oAuthAccountRepository
            .findActiveByProviderAndProviderId(profile.provider, profile.providerId)
            ?.let { userRepository.findById(it.userId) }

    fun getActiveOAuthAccount(userId: Long): OAuthAccount =
        oAuthAccountRepository.findActiveByUserId(userId) ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

    fun createOAuthUser(
        profile: OAuthUserProfile,
        nickname: String,
    ): User? {
        val now = LocalDateTime.now()
        return createUserWithOAuthAccount(profile, nickname, now)
    }

    private fun createUserWithOAuthAccount(
        profile: OAuthUserProfile,
        nickname: String,
        now: LocalDateTime,
    ): User? {
        val user = userRepository.create(nickname, now) ?: return null
        createOAuthAccount(user, profile, now)
        return user
    }

    fun updateOAuthLogin(
        user: User,
        profile: OAuthUserProfile,
    ): User {
        val account = getActiveOAuthAccount(profile)
        validateOAuthAccountMatchesUser(account, user)
        val loginUser =
            if (user.status == UserStatus.WITHDRAWAL_REQUESTED) {
                userRepository.reactivateWithdrawalRequested(user.id, LocalDateTime.now()) ?: user
            } else {
                user
            }
        validateAuthenticatableStatus(loginUser)
        updateOAuthAccountLogin(account, profile)
        return loginUser
    }

    private fun createOAuthAccount(
        user: User,
        profile: OAuthUserProfile,
        now: LocalDateTime,
    ) {
        oAuthAccountRepository.create(user.id, profile, now) ?: throw CustomException(ErrorCode.INTERNAL_SERVER_ERROR)
    }

    private fun getActiveOAuthAccount(profile: OAuthUserProfile): OAuthAccount =
        oAuthAccountRepository.findActiveByProviderAndProviderId(profile.provider, profile.providerId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

    private fun updateOAuthAccountLogin(
        account: OAuthAccount,
        profile: OAuthUserProfile,
    ) {
        oAuthAccountRepository.updateLogin(account.id, profile, LocalDateTime.now())
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)
    }

    private fun validateOAuthAccountMatchesUser(
        account: OAuthAccount,
        user: User,
    ) {
        if (account.userId == user.id) return
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
            UserStatus.WITHDRAWAL_REQUESTED -> ErrorCode.AUTH_USER_DELETED
            UserStatus.DELETED -> ErrorCode.AUTH_USER_DELETED
        }
}
