package com.firstpenguin.app.global.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val message: String,
) {
    // Common
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다"),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않는 HTTP 메서드입니다"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    EMPTY_REQUEST_BODY(HttpStatus.BAD_REQUEST, "요청 본문이 비어있습니다."),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다"),
    ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Access Token이 만료되었습니다"),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 Access Token입니다"),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Refresh Token이 만료되었습니다"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다"),
    OAUTH_PROVIDER_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "지원하지 않는 OAuth provider입니다"),
    REFRESH_TOKEN_REQUIRED(HttpStatus.UNAUTHORIZED, "Refresh Token이 필요합니다"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),

    // Emotion
    NOT_FOUND_EMOTION_RANGE(HttpStatus.NOT_FOUND, "일치하는 감정 분류가 존재하지 않습니다."),
    INVALID_EMOTION_RANGE_NAME(HttpStatus.INTERNAL_SERVER_ERROR, "감정 분류 데이터가 올바르지 않습니다."),
    INVALID_TAG_TYPE(HttpStatus.INTERNAL_SERVER_ERROR, "태그 타입 데이터가 올바르지 않습니다."),
    INVALID_EMOTION_TAG(HttpStatus.BAD_REQUEST, "유효하지 않은 감정 태그가 포함되어 있습니다."),
    INVALID_TONE_TAG(HttpStatus.BAD_REQUEST, "유효하지 않은 톤 태그가 포함되어 있습니다."),
    INVALID_EMOTION_TAG_RANGE(HttpStatus.BAD_REQUEST, "선택한 감정 태그는 동일한 감정 범위에 속해야 합니다."),

    // Image
    UNSUPPORTED_IMAGE_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "허용되지 않는 이미지 형식입니다"),

    // Diary
    DIARY_NOT_FOUND(HttpStatus.NOT_FOUND, "일기를 찾을 수 없습니다"),
    DIARY_UPDATE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "오늘 작성한 일기만 수정할 수 있습니다"),
    DIARY_DELETE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "오늘 작성한 일기만 삭제할 수 있습니다"),
}
