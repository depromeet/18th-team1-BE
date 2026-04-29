package com.firstpenguin.app.global.response

import com.firstpenguin.app.global.exception.ErrorCode

data class ErrorResponse(
    val message: String,
) {
    companion object {
        fun of(errorCode: ErrorCode) = ErrorResponse(errorCode.message)

        fun of(message: String) = ErrorResponse(message)
    }
}
