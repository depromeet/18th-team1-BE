package com.firstpenguin.app.global.response

import com.firstpenguin.app.global.exception.ErrorCode

data class ErrorResponse(
    val code: String,
    val message: String,
) {
    companion object {
        fun of(errorCode: ErrorCode) = ErrorResponse(errorCode.code, errorCode.message)

        fun of(
            code: String,
            message: String,
        ) = ErrorResponse(code, message)
    }
}
