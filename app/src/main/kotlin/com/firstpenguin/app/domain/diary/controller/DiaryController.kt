package com.firstpenguin.app.domain.diary.controller

import com.firstpenguin.app.domain.auth.model.AuthenticatedUser
import com.firstpenguin.app.domain.diary.usecase.DiaryUseCase
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import com.firstpenguin.app.global.response.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/diaries")
@Tag(name = "일기", description = "로그인한 사용자의 일기 API")
class DiaryController(
    private val diaryUseCase: DiaryUseCase,
) {
    @GetMapping("/{diaryId}/share", produces = [MediaType.IMAGE_PNG_VALUE])
    @Operation(
        summary = "일기 공유 이미지 생성",
        description = SHARE_IMAGE_DESCRIPTION,
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "일기 공유 이미지 생성 성공",
                content = [
                    Content(mediaType = MediaType.IMAGE_PNG_VALUE),
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
            ApiResponse(
                responseCode = "403",
                description = "다른 사용자의 일기에는 접근할 수 없습니다.",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "일기를 찾을 수 없습니다.",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
    fun generateShareImage(
        @Parameter(hidden = true)
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser?,
        @Parameter(description = "일기 ID", example = "1")
        @PathVariable
        diaryId: Long,
    ): ResponseEntity<ByteArray> {
        if (authenticatedUser == null) {
            throw CustomException(ErrorCode.UNAUTHORIZED)
        }

        val image =
            diaryUseCase.generateShareImage(
                userId = authenticatedUser.id,
                diaryId = diaryId,
            )

        return ResponseEntity
            .ok()
            .contentType(MediaType.IMAGE_PNG)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"diary-share-$diaryId.png\"")
            .body(image)
    }

    private companion object {
        const val SHARE_IMAGE_DESCRIPTION =
            "Authorization 헤더에 `Bearer {accessToken}` 형식으로 access token을 담아 호출합니다. " +
                "일기의 작성일, 연결된 문장, 책 제목과 저자를 사용해 PNG 공유 이미지를 생성합니다."
    }
}
