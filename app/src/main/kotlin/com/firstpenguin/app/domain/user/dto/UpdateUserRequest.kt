package com.firstpenguin.app.domain.user.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "프로필 수정 요청. nickname, profileImageId 중 하나 이상 필수. null 필드는 변경하지 않습니다.")
data class UpdateUserRequest(
    @field:Schema(description = "변경할 닉네임. null이면 변경하지 않음. 예약 닉네임은 사용할 수 없음", example = "새닉네임")
    val nickname: String?,
    @field:Schema(description = "변경할 프로필 이미지 ID. null이면 변경하지 않음. 이미지 제거는 현재 미지원", example = "42")
    val profileImageId: Long?,
)
