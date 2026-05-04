package com.firstpenguin.app.domain.image.controller

import com.firstpenguin.app.domain.image.dto.PresignedUrlRequest
import com.firstpenguin.app.domain.image.dto.PresignedUrlResponse
import com.firstpenguin.app.domain.image.service.ImageService
import com.firstpenguin.app.global.response.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/images")
@Tag(name = "이미지", description = "이미지 업로드 API")
class ImageController(
    private val imageService: ImageService,
) {
    @PostMapping("/presigned-url")
    @Operation(
        summary = "이미지 업로드용 presigned URL 발급",
        description = PRESIGNED_URL_DESCRIPTION,
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "presigned URL 발급 성공",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = PresignedUrlResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "지원하지 않는 contentType입니다. (image/jpeg, image/png, image/webp만 허용)",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                description = "access token이 없거나, 만료되었거나, 유효하지 않습니다.",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
    fun issuePresignedUrl(
        @RequestBody request: PresignedUrlRequest,
    ): ResponseEntity<PresignedUrlResponse> {
        val (presignedUrl, imageId) = imageService.issue(request.type, request.contentType)
        return ResponseEntity.ok(PresignedUrlResponse(presignedUrl, imageId))
    }

    private companion object {
        const val PRESIGNED_URL_DESCRIPTION =
            "GCS에 이미지를 직접 업로드하기 위한 presigned URL과 imageId를 반환합니다. " +
                "클라이언트는 presigned URL로 GCS에 PUT 업로드한 뒤, " +
                "도메인 생성 API 호출 시 imageId를 함께 전달합니다. " +
                "contentType은 image/jpeg, image/png, image/webp만 허용합니다."
    }
}
