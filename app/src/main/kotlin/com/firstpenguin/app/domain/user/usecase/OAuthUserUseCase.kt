package com.firstpenguin.app.domain.user.usecase

import com.firstpenguin.app.domain.user.model.OAuthUserProfile
import com.firstpenguin.app.domain.user.model.User
import com.firstpenguin.app.domain.user.service.UserService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OAuthUserUseCase(
    private val userService: UserService,
) {
    @Transactional
    fun upsertOAuthUser(profile: OAuthUserProfile): User = userService.upsertOAuthUser(profile)
}
