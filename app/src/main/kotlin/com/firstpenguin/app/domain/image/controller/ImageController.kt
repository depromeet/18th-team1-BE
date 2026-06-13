package com.firstpenguin.app.domain.image.controller

import com.firstpenguin.app.domain.auth.model.AuthenticatedUser
import com.firstpenguin.app.domain.image.dto.PresignedUrlRequest
import com.firstpenguin.app.domain.image.dto.PresignedUrlResponse
import com.firstpenguin.app.domain.image.usecase.ImageUseCase
import com.firstpenguin.app.global.response.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/images")
@Tag(name = "이미지", description = "이미지 업로드 API")
class ImageController(
    private val imageUseCase: ImageUseCase,
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
    ): ResponseEntity<PresignedUrlResponse> = ResponseEntity.ok(imageUseCase.issuePresignedUrl(request))

    @GetMapping("/share/calendar", produces = [MediaType.IMAGE_PNG_VALUE])
    @Operation(
        summary = "캘린더 공유 이미지 생성",
        description = CALENDAR_DESCRIPTION,
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "이미지 생성 성공",
                content = [Content(mediaType = MediaType.IMAGE_PNG_VALUE)],
            ),
            ApiResponse(
                responseCode = "400",
                description = "지원하지 않는 type입니다. (4, 5만 허용)",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
    fun generateCalendarShareImage(
        @Parameter(hidden = true) @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @Parameter(description = "스타일 타입 (4 또는 5)", example = "4") @RequestParam type: Int,
        @Parameter(description = "연도", example = "2026") @RequestParam year: Int,
        @Parameter(description = "월 (1~12)", example = "11") @RequestParam month: Int,
    ): ResponseEntity<ByteArray> =
        ResponseEntity
            .ok()
            .contentType(MediaType.IMAGE_PNG)
            .body(imageUseCase.generateCalendarShareImage(authenticatedUser.id, type, year, month))

    @GetMapping("/share/quote", produces = [MediaType.IMAGE_PNG_VALUE])
    @Operation(
        summary = "문장 공유 이미지 생성",
        description = QUOTE_SHARE_DESCRIPTION,
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "문장 공유 이미지 생성 성공",
                content = [Content(mediaType = MediaType.IMAGE_PNG_VALUE)],
            ),
            ApiResponse(
                responseCode = "400",
                description = "지원하지 않는 type입니다. (1~3만 허용)",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
    fun generateQuoteShareImage(
        @Parameter(description = "문장 공유 이미지 타입 (1~3)", example = "1") @RequestParam type: Int,
        @Parameter(description = "작성일", example = "2026-11-26")
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        createdAt: LocalDate,
        @Parameter(description = "공유할 문장", example = "세상에는 두 종류의 고통이 있다.")
        @RequestParam
        quote: String,
        @Parameter(description = "책 제목", example = "아픔이 길이 되려면")
        @RequestParam
        title: String,
        @Parameter(description = "작가", example = "김승섭")
        @RequestParam
        author: String,
        @Parameter(
            description = "책 커버 이미지 URL. type 3에서만 필수입니다.",
            example = "https://image.aladin.co.kr/product/38850/38/cover200/k562137707_1.jpg",
        )
        @RequestParam(required = false)
        coverImageUrl: String?,
    ): ResponseEntity<ByteArray> =
        ResponseEntity
            .ok()
            .contentType(MediaType.IMAGE_PNG)
            .body(
                imageUseCase.generateQuoteShareImage(
                    type = type,
                    createdAt = createdAt,
                    quote = quote,
                    title = title,
                    author = author,
                    coverImageUrl = coverImageUrl,
                ),
            )

    private companion object {
        const val QUOTE_SHARE_DESCRIPTION =
            "문장, 책 제목, 작가, 작성일을 사용해 문장 공유 이미지 PNG를 생성합니다. " +
                "type은 1, 2, 3 중 하나를 지정합니다. createdAt은 yyyy-MM-dd 형식입니다. " +
                "coverImageUrl은 type 3에서만 필수입니다."

        const val CALENDAR_DESCRIPTION =
            "Authorization 헤더에 `Bearer {accessToken}` 형식으로 access token을 담아 호출합니다. " +
                "해당 연월에 작성한 일기를 날짜별로 묶어 책 커버 캘린더 이미지를 반환합니다. " +
                "type은 4 또는 5 중 하나를 지정합니다."

        const val PRESIGNED_URL_DESCRIPTION =
            "GCS에 이미지를 직접 업로드하기 위한 presigned URL과 imageId를 반환합니다. " +
                "클라이언트는 presigned URL로 GCS에 PUT 업로드한 뒤, 도메인 생성/수정 API 호출 시 imageId를 함께 전달합니다. " +
                "type은 DIARY(일기), USER_PROFILE(프로필 이미지), REPORT(신고) 중 하나를 지정합니다. " +
                "contentType은 image/jpeg, image/png, image/webp만 허용합니다."
    }
}
