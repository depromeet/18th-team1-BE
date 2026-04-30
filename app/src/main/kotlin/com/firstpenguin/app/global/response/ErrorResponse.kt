package com.firstpenguin.app.global.response

import com.firstpenguin.app.global.exception.ErrorCode
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "에러 응답")
data class ErrorResponse(
    @field:Schema(description = "프론트에 표시하거나 분기할 에러 메시지", example = "Access Token이 만료되었습니다")
    val message: String,
) {
    companion object {
        fun of(errorCode: ErrorCode) = ErrorResponse(errorCode.message)

        fun of(message: String) = ErrorResponse(message)
    }
}
