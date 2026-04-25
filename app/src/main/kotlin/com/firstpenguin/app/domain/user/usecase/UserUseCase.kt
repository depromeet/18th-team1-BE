package com.firstpenguin.app.domain.user.usecase

import com.firstpenguin.app.domain.user.dto.UserResponse
import com.firstpenguin.app.domain.user.repository.UserRepository
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserUseCase(
    private val userRepository: UserRepository,
) {
    @Transactional(readOnly = true)
    fun getMe(userId: Long): UserResponse {
        val user = userRepository.findById(userId) ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        return UserResponse.from(user)
    }
}
