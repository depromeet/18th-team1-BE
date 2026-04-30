package com.firstpenguin.app.global.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val code: String,
    val message: String,
) {
    // Common
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C001", "서버 내부 오류가 발생했습니다"),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C002", "잘못된 입력값입니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C003", "허용되지 않는 HTTP 메서드입니다"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "C004", "요청한 리소스를 찾을 수 없습니다"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, code = "C005", message = "요청 값이 올바르지 않습니다."),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증이 필요합니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "A002", "접근 권한이 없습니다"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "A003", "토큰이 만료되었습니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A004", "유효하지 않은 토큰입니다"),

    // Emotion
    NOT_FOUND_EMOTION_RANGE(HttpStatus.NOT_FOUND, "E001", "일치하는 감정 분류가 존재하지 않습니다."),
    INVALID_EMOTION_RANGE_NAME(HttpStatus.INTERNAL_SERVER_ERROR, "E002", "감정 분류 데이터가 올바르지 않습니다.")
}
