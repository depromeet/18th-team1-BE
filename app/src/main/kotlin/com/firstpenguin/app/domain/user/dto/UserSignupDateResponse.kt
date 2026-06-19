package com.firstpenguin.app.domain.user.dto

import com.firstpenguin.app.domain.user.model.User
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "사용자 가입일 응답")
data class UserSignupDateResponse(
    @field:Schema(description = "가입한 날짜", example = "2026-06-13")
    val signupDate: LocalDate,
) {
    companion object {
        fun from(user: User): UserSignupDateResponse = UserSignupDateResponse(signupDate = user.createdAt.toLocalDate())
    }
}
