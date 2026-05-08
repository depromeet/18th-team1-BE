package com.firstpenguin.app.domain.image.dto

import com.firstpenguin.app.domain.image.model.ImageType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "presigned URL 발급 요청")
data class PresignedUrlRequest(
    @field:Schema(
        description = "이미지 타입. DIARY(일기), USER_PROFILE(프로필 이미지), REPORT(신고) 중 하나",
        example = "DIARY",
        allowableValues = ["DIARY", "USER_PROFILE", "REPORT"],
    )
    val type: ImageType,
    @field:Schema(description = "이미지 MIME 타입. image/jpeg, image/png, image/webp만 허용", example = "image/jpeg")
    val contentType: String,
)
