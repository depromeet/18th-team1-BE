package com.firstpenguin.app.domain.user.usecase

import com.firstpenguin.app.domain.image.service.ImageService
import com.firstpenguin.app.domain.user.dto.UserResponse
import com.firstpenguin.app.domain.user.service.UserService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserUseCase(
    private val imageService: ImageService,
    private val userService: UserService,
) {
    @Transactional(readOnly = true)
    fun getMe(userId: Long): UserResponse {
        val user = userService.getById(userId)
        val profileImageUrl = user.profileImageId?.let(imageService::findUrlById)

        return UserResponse.from(user, profileImageUrl)
    }
}
