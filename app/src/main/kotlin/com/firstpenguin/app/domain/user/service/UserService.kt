package com.firstpenguin.app.domain.user.service

import com.firstpenguin.app.domain.user.model.OAuthUserProfile
import com.firstpenguin.app.domain.user.model.User
import com.firstpenguin.app.domain.user.repository.UserRepository
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
) {
    fun getById(id: Long): User = userRepository.findById(id) ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

    fun upsertOAuthUser(profile: OAuthUserProfile): User = userRepository.upsertOAuthUser(profile)
}
