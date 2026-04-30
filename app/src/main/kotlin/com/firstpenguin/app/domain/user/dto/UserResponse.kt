package com.firstpenguin.app.domain.user.dto

import com.firstpenguin.app.domain.user.model.User

data class UserResponse(
    val id: Long,
    val email: String?,
    val nickname: String,
    val profileImageUrl: String?,
) {
    companion object {
        fun from(
            user: User,
            profileImageUrl: String?,
        ): UserResponse =
            UserResponse(
                id = user.id,
                email = user.email,
                nickname = user.nickname,
                profileImageUrl = profileImageUrl,
            )
    }
}
