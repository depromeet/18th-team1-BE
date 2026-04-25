package com.firstpenguin.app.domain.user.dto

import com.firstpenguin.app.domain.user.model.Role
import com.firstpenguin.app.domain.user.model.User
import com.firstpenguin.app.domain.user.model.UserStatus

data class UserResponse(
    val id: Long,
    val email: String?,
    val nickname: String,
    val profileImageKey: String?,
    val status: UserStatus,
    val role: Role,
) {
    companion object {
        fun from(user: User): UserResponse =
            UserResponse(
                id = user.id,
                email = user.email,
                nickname = user.nickname,
                profileImageKey = user.profileImageKey,
                status = user.status,
                role = user.role,
            )
    }
}
