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
    AUTH_USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "인증된 사용자를 찾을 수 없습니다"),
    AUTH_USER_DELETED(HttpStatus.UNAUTHORIZED, "탈퇴한 사용자입니다"),
    AUTH_USER_BLOCKED(HttpStatus.UNAUTHORIZED, "차단된 사용자입니다"),
    ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Access Token이 만료되었습니다"),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 Access Token입니다"),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Refresh Token이 만료되었습니다"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다"),
    OAUTH_PROVIDER_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "지원하지 않는 OAuth provider입니다"),
    REFRESH_TOKEN_REQUIRED(HttpStatus.UNAUTHORIZED, "Refresh Token이 필요합니다"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다"),
    NICKNAME_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "사용 가능한 닉네임 생성에 실패했습니다"),

    // Emotion
    EMOTION_RANGE_NOT_FOUND(HttpStatus.NOT_FOUND, "일치하는 감정 분류가 존재하지 않습니다."),
    INVALID_EMOTION_RANGE_NAME(HttpStatus.INTERNAL_SERVER_ERROR, "감정 분류 데이터가 올바르지 않습니다."),
    INVALID_TAG_TYPE(HttpStatus.INTERNAL_SERVER_ERROR, "태그 타입 데이터가 올바르지 않습니다."),
    INVALID_EMOTION_TAG(HttpStatus.BAD_REQUEST, "유효하지 않은 감정 태그가 포함되어 있습니다."),
    INVALID_NEED_TAG(HttpStatus.BAD_REQUEST, "유효하지 않은 기대 태그가 포함되어 있습니다."),
    INVALID_EMOTION_TAG_RANGE(HttpStatus.BAD_REQUEST, "선택한 감정 태그는 동일한 감정 범위에 속해야 합니다."),

    // Image
    UNSUPPORTED_IMAGE_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "허용되지 않는 이미지 형식입니다"),

    // Diary
    DIARY_NOT_FOUND(HttpStatus.NOT_FOUND, "일기를 찾을 수 없습니다"),

    // Recommendation
    RECOMMENDATION_NOT_FOUND(HttpStatus.NOT_FOUND, "추천 기록이 존재하지 않습니다."),
    INVALID_RECOMMENDATION_QUOTE(HttpStatus.BAD_REQUEST, "추천받은 문장만 선택할 수 있습니다."),
    EXCEEDED_RECOMMENDATION_QUOTE_LIMIT(HttpStatus.CONFLICT, "추천 문장 더보기 개수를 초과했습니다."),
    FORBIDDEN_RECOMMENDATION(HttpStatus.FORBIDDEN, "본인의 추천 기록만 사용할 수 있습니다."),
    RECOMMENDATION_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "오늘 추천은 최대 5개까지 생성할 수 있습니다."),
    RECOMMENDATION_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "추천 기록 생성에 실패했습니다."),
    RECOMMENDATION_ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 문장을 선택한 추천입니다."),
    RECOMMENDATION_NOT_COMPLETED(HttpStatus.CONFLICT, "아직 문장을 선택하지 않은 추천입니다."),
    INVALID_RECOMMENDATION_NEED_INPUT(HttpStatus.BAD_REQUEST, "기대 태그 또는 직접 입력 문장 중 하나만 선택해야 합니다."),

    // Book
    BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "책이 존재하지 않습니다."),

    // Quote
    NOT_ENOUGH_QUOTES(HttpStatus.CONFLICT, "추천 가능한 문장이 부족합니다."),
    QUOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "문장을 찾을 수 없습니다."),

    // OpenAi
    BATCH_ADMIN_SECRET_REQUIRED(HttpStatus.INTERNAL_SERVER_ERROR, "배치 관리자 secret 설정이 누락되었습니다."),
    OPENAI_API_KEY_REQUIRED(HttpStatus.INTERNAL_SERVER_ERROR, "OpenAI API 키 설정이 누락되었습니다."),
    OPENAI_FILE_UPLOAD_FAILED(HttpStatus.BAD_GATEWAY, "OpenAI 파일 업로드에 실패했습니다."),
    OPENAI_BATCH_CREATE_FAILED(HttpStatus.BAD_GATEWAY, "OpenAI 배치 생성에 실패했습니다."),
    OPENAI_BATCH_STATUS_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "OpenAI 배치 상태 조회에 실패했습니다."),
    QUOTE_BATCH_JOB_IS_RUNNING(HttpStatus.CONFLICT, "현재 배치 작업이 진행 중입니다."),
    INVALID_QUOTE_BATCH_JOB_STATUS(HttpStatus.INTERNAL_SERVER_ERROR, "문구 배치 상태가 올바르지 않습니다."),
    INVALID_QUOTE_BATCH_ITEMS_STATUS(HttpStatus.INTERNAL_SERVER_ERROR, "문구 배치 대상 상태가 올바르지 않습니다."),

    // QuoteMetadataBatch
    QUOTE_METADATA_BATCH_JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "문구 메타정보 배치 작업이 존재하지 않습니다."),
    QUOTE_METADATA_BATCH_TARGET_NOT_FOUND(HttpStatus.NOT_FOUND, "문구 메타정보 배치 대상이 존재하지 않습니다."),
    QUOTE_METADATA_BATCH_OPENAI_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "OpenAI 문구 메타정보 배치 요청에 실패했습니다."),
    QUOTE_METADATA_BATCH_OUTPUT_TEXT_NOT_FOUND(HttpStatus.BAD_GATEWAY, "OpenAI 문구 메타정보 배치 결과를 찾을 수 없습니다."),

    // QuoteCreationBatch
    QUOTE_CREATION_BATCH_JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "추천 문장 생성 배치 작업이 존재하지 않습니다."),
    QUOTE_EXTRACTION_BATCH_TARGET_NOT_FOUND(HttpStatus.NOT_FOUND, "인용구 추출 배치 대상이 존재하지 않습니다."),
    QUOTE_REVIEW_BATCH_TARGET_NOT_FOUND(HttpStatus.NOT_FOUND, "추천 문장 후보 검수 배치 대상이 존재하지 않습니다."),
    QUOTE_EXTRACTION_BATCH_OPENAI_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "OpenAI 인용구 추출 배치 요청에 실패했습니다."),
    QUOTE_CREATION_BATCH_OUTPUT_TEXT_NOT_FOUND(HttpStatus.BAD_GATEWAY, "OpenAI 추천 문장 생성 배치 결과를 찾을 수 없습니다."),

    // QuoteEmbedding
    INVALID_QUOTE_EMBEDDING_INPUT(HttpStatus.BAD_REQUEST, "임베딩할 문장 입력이 올바르지 않습니다."),
    QUOTE_EMBEDDING_OPENAI_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "OpenAI 임베딩 요청에 실패했습니다."),
    QUOTE_EMBEDDING_RESPONSE_EMPTY(HttpStatus.BAD_GATEWAY, "OpenAI 임베딩 응답이 비어 있습니다."),
    QUOTE_EMBEDDING_RESPONSE_SIZE_MISMATCH(HttpStatus.BAD_GATEWAY, "OpenAI 임베딩 응답 개수가 요청 개수와 다릅니다."),
    QUOTE_EMBEDDING_DIMENSION_MISMATCH(HttpStatus.BAD_GATEWAY, "OpenAI 임베딩 벡터 차원이 올바르지 않습니다."),

    // Image
    INVALID_IMAGE_OWNER_TYPE(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 주인 타입이 올바르지 않습니다."),
    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "이미지가 존재하지 않습니다."),
}
