package com.firstpenguin.app.domain.image.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "presigned URL 발급 응답")
data class PresignedUrlResponse(
    @field:Schema(description = "GCS PUT 업로드용 presigned URL (15분 유효)", example = "https://storage.googleapis.com/bucket/diary/uuid.jpg?X-Goog-Signature=...")
    val presignedUrl: String,
    @field:Schema(description = "업로드된 이미지의 ID. 도메인 생성 시 전달", example = "1")
    val imageId: Long,
)
