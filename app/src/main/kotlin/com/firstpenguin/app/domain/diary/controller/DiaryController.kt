package com.firstpenguin.app.domain.diary.controller

import com.firstpenguin.app.domain.auth.model.AuthenticatedUser
import com.firstpenguin.app.domain.diary.dto.DiaryDetailResponse
import com.firstpenguin.app.domain.diary.dto.DiaryPeriodResponse
import com.firstpenguin.app.domain.diary.dto.UpdateDiaryContentRequest
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
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/diaries")
@Tag(name = "일기", description = "로그인한 사용자의 일기 API")
class DiaryController(
    private val diaryUseCase: DiaryUseCase,
) {
    @DeleteMapping("/{diaryId}")
    @Operation(
        summary = "일기 삭제",
        description = DELETE_DESCRIPTION,
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "일기 삭제 성공"),
            ApiResponse(
                responseCode = "400",
                description = "오늘 작성한 일기가 아닙니다.",
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
    fun deleteDiary(
        @Parameter(hidden = true)
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser?,
        @Parameter(description = "일기 ID", example = "1")
        @PathVariable
        diaryId: Long,
    ): ResponseEntity<Unit> {
        if (authenticatedUser == null) {
            throw CustomException(ErrorCode.UNAUTHORIZED)
        }

        diaryUseCase.deleteDiary(
            userId = authenticatedUser.id,
            diaryId = diaryId,
        )

        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/{diaryId}")
    @Operation(
        summary = "일기 내용 수정",
        description = UPDATE_DESCRIPTION,
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "일기 내용 수정 성공",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = DiaryDetailResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "일기 내용이 비어 있거나 오늘 작성한 일기가 아닙니다.",
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
    fun updateDiaryContent(
        @Parameter(hidden = true)
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser?,
        @Parameter(description = "일기 ID", example = "1")
        @PathVariable
        diaryId: Long,
        @Valid
        @RequestBody
        request: UpdateDiaryContentRequest,
    ): DiaryDetailResponse {
        if (authenticatedUser == null) {
            throw CustomException(ErrorCode.UNAUTHORIZED)
        }

        return diaryUseCase.updateDiaryContent(
            userId = authenticatedUser.id,
            diaryId = diaryId,
            request = request,
        )
    }

    @GetMapping("/{diaryId}")
    @Operation(
        summary = "일기 상세 조회",
        description = DETAIL_DESCRIPTION,
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "일기 상세 조회 성공",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = DiaryDetailResponse::class),
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
    fun getDiary(
        @Parameter(hidden = true)
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser?,
        @Parameter(description = "일기 ID", example = "1")
        @PathVariable
        diaryId: Long,
    ): DiaryDetailResponse {
        if (authenticatedUser == null) {
            throw CustomException(ErrorCode.UNAUTHORIZED)
        }

        return diaryUseCase.getDiary(
            userId = authenticatedUser.id,
            diaryId = diaryId,
        )
    }

    @GetMapping
    @Operation(
        summary = "기간별 일기 조회",
        description = PERIOD_DESCRIPTION,
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "기간별 일기 조회 성공",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = DiaryPeriodResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "start가 end보다 늦거나 날짜 형식이 잘못되었습니다.",
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
    fun getDiariesByPeriod(
        @Parameter(hidden = true)
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser?,
        @Parameter(description = "조회 시작일", example = "2026-05-01")
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        start: LocalDate,
        @Parameter(description = "조회 종료일", example = "2026-05-31")
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        end: LocalDate,
    ): DiaryPeriodResponse {
        if (authenticatedUser == null) {
            throw CustomException(ErrorCode.UNAUTHORIZED)
        }

        return diaryUseCase.getDiariesByPeriod(
            userId = authenticatedUser.id,
            start = start,
            end = end,
        )
    }

    private companion object {
        const val DELETE_DESCRIPTION =
            "Authorization 헤더에 `Bearer {accessToken}` 형식으로 access token을 담아 호출합니다. " +
                "오늘 작성한 내 일기만 삭제할 수 있으며, `deleted_at`을 갱신하는 soft delete로 처리합니다."

        const val UPDATE_DESCRIPTION =
            "Authorization 헤더에 `Bearer {accessToken}` 형식으로 access token을 담아 호출합니다. " +
                "오늘 작성한 내 일기의 `content`만 수정할 수 있습니다."

        const val DETAIL_DESCRIPTION =
            "Authorization 헤더에 `Bearer {accessToken}` 형식으로 access token을 담아 호출합니다. " +
                "삭제되지 않은 내 일기만 조회합니다."

        const val PERIOD_DESCRIPTION =
            "Authorization 헤더에 `Bearer {accessToken}` 형식으로 access token을 담아 호출합니다. " +
                "`start`와 `end`는 `yyyy-MM-dd` 형식이며, `end` 날짜의 23:59:59까지 포함합니다. " +
                "삭제되지 않은 내 일기만 `created_at` 오름차순으로 조회합니다."
    }
}
