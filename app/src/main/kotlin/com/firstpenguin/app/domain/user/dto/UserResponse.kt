package com.firstpenguin.app.domain.user.dto

import com.firstpenguin.app.domain.user.model.User
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "내 정보 응답")
data class UserResponse(
    @field:Schema(description = "사용자 ID", example = "1")
    val id: Long,
    @field:Schema(description = "OAuth provider에서 제공한 이메일. 제공되지 않으면 null", example = "user@example.com")
    val email: String?,
    @field:Schema(description = "사용자 닉네임", example = "산타는펭귄")
    val nickname: String,
    @field:Schema(description = "프로필 이미지 URL. 연결된 이미지가 없으면 null", example = "https://cdn.example.com/profile.png")
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
