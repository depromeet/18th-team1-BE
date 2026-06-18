package com.firstpenguin.app.domain.user.usecase

import com.firstpenguin.app.domain.auth.service.RefreshTokenService
import com.firstpenguin.app.domain.image.service.ImageService
import com.firstpenguin.app.domain.user.dto.UpdateUserRequest
import com.firstpenguin.app.domain.user.dto.UserResponse
import com.firstpenguin.app.domain.user.service.OAuthUserService
import com.firstpenguin.app.domain.user.service.UserService
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserUseCase(
    private val imageService: ImageService,
    private val oAuthUserService: OAuthUserService,
    private val refreshTokenService: RefreshTokenService,
    private val userService: UserService,
) {
    @Transactional(readOnly = true)
    fun getMe(userId: Long): UserResponse {
        val user = userService.getById(userId)
        val oAuthAccount = oAuthUserService.getActiveOAuthAccount(userId)
        val profileImageUrl = user.profileImageId?.let(imageService::findUrlById)

        return UserResponse.from(user, oAuthAccount, profileImageUrl)
    }

    @Transactional
    fun updateMe(
        userId: Long,
        request: UpdateUserRequest,
    ): UserResponse {
        if (request.nickname == null && request.profileImageId == null) {
            throw CustomException(ErrorCode.INVALID_INPUT)
        }
        request.profileImageId?.let { imageService.validateExists(it) }
        userService.updateProfile(userId, request.nickname, request.profileImageId)
        return getMe(userId)
    }

    @Transactional
    fun withdrawMe(userId: Long) {
        userService.requestWithdrawal(userId)
        refreshTokenService.logoutAll(userId)
    }
}
