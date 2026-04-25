package com.firstpenguin.app.domain.user.controller

import com.firstpenguin.app.domain.auth.model.AuthenticatedUser
import com.firstpenguin.app.domain.user.dto.UserResponse
import com.firstpenguin.app.domain.user.usecase.UserUseCase
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class UserController(
    private val userUseCase: UserUseCase,
) {
    @GetMapping("/me")
    fun me(
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser?,
    ): UserResponse {
        if (authenticatedUser == null) {
            throw CustomException(ErrorCode.UNAUTHORIZED)
        }

        return userUseCase.getMe(authenticatedUser.id)
    }
}
