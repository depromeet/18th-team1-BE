package com.firstpenguin.app.domain.quote.controller

import com.firstpenguin.app.domain.auth.model.AuthenticatedUser
import com.firstpenguin.app.domain.quote.dto.BulkDeleteQuoteScrapsRequest
import com.firstpenguin.app.domain.quote.dto.ScrappedQuotesResponse
import com.firstpenguin.app.domain.quote.usecase.QuoteScrapUseCase
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
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "문장 스크랩", description = "문장 스크랩 생성, 취소, 목록 조회 API")
class QuoteScrapController(
    private val quoteScrapUseCase: QuoteScrapUseCase,
) {
    @PutMapping("/quotes/{quoteId}/scrap")
    @Operation(
        summary = "문장 스크랩 상태 설정 API",
        description = "로그인 사용자의 문장 스크랩 상태를 켠다. 이미 스크랩한 문장도 성공 처리한다.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "스크랩 상태 설정 성공. 응답 본문은 없습니다."),
            ApiResponse(
                responseCode = "401",
                description = UNAUTHORIZED_DESCRIPTION,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = QUOTE_NOT_FOUND_DESCRIPTION,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
    fun setQuoteScrap(
        @Parameter(hidden = true) @AuthenticationPrincipal authenticatedUser: AuthenticatedUser?,
        @Parameter(description = "문장 ID", example = "1") @PathVariable quoteId: Long,
    ): ResponseEntity<Unit> {
        val user = authenticatedUser ?: throw CustomException(ErrorCode.UNAUTHORIZED)
        quoteScrapUseCase.setQuoteScrap(user.id, quoteId)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/quotes/{quoteId}/scrap")
    @Operation(
        summary = "문장 스크랩 취소 API",
        description = "로그인 사용자의 문장 스크랩 상태를 끈다. 스크랩하지 않은 문장도 성공 처리한다.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "스크랩 취소 성공. 응답 본문은 없습니다."),
            ApiResponse(
                responseCode = "401",
                description = UNAUTHORIZED_DESCRIPTION,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = QUOTE_NOT_FOUND_DESCRIPTION,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
    fun deleteQuoteScrap(
        @Parameter(hidden = true) @AuthenticationPrincipal authenticatedUser: AuthenticatedUser?,
        @Parameter(description = "문장 ID", example = "1") @PathVariable quoteId: Long,
    ): ResponseEntity<Unit> {
        val user = authenticatedUser ?: throw CustomException(ErrorCode.UNAUTHORIZED)
        quoteScrapUseCase.deleteQuoteScrap(user.id, quoteId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/my-page/scrapped-quotes")
    @Operation(
        summary = "마이페이지 스크랩 문장 목록 조회 API",
        description = SCRAPPED_QUOTES_DESCRIPTION,
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "스크랩 문장 목록 조회 성공",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = ScrappedQuotesResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "cursor 형식이 올바르지 않거나 limit가 1~50 범위를 벗어났습니다.",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                description = UNAUTHORIZED_DESCRIPTION,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
    fun getScrappedQuotes(
        @Parameter(hidden = true) @AuthenticationPrincipal authenticatedUser: AuthenticatedUser?,
        @Parameter(
            description = SCRAPPED_QUOTES_CURSOR_DESCRIPTION,
            required = false,
            example = "MjAyNi0wNi0xM1QxNDozMDowMHwx",
        )
        @RequestParam(required = false) cursor: String?,
        @Parameter(
            description = "조회 개수. 생략 시 10개를 조회하고, 최대 50개까지 허용합니다.",
            example = "10",
            required = false,
            schema = Schema(defaultValue = "10", minimum = "1", maximum = "50"),
        )
        @RequestParam(defaultValue = "10") limit: Int,
    ): ResponseEntity<ScrappedQuotesResponse> {
        val user = authenticatedUser ?: throw CustomException(ErrorCode.UNAUTHORIZED)
        return ResponseEntity.ok(quoteScrapUseCase.getScrappedQuotes(user.id, cursor, limit))
    }

    @PostMapping("/quote-scraps/bulk-delete")
    @Operation(
        summary = "문장 스크랩 다중 취소 API",
        description = BULK_DELETE_DESCRIPTION,
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "다중 스크랩 취소 성공. 응답 본문은 없습니다."),
            ApiResponse(
                responseCode = "400",
                description = "quoteIds가 비어 있거나, 50개를 초과했거나, 1 미만의 ID가 포함되어 있습니다.",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                description = UNAUTHORIZED_DESCRIPTION,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
    fun bulkDeleteQuoteScraps(
        @Parameter(hidden = true) @AuthenticationPrincipal authenticatedUser: AuthenticatedUser?,
        @Valid @RequestBody request: BulkDeleteQuoteScrapsRequest,
    ): ResponseEntity<Unit> {
        val user = authenticatedUser ?: throw CustomException(ErrorCode.UNAUTHORIZED)
        quoteScrapUseCase.deleteQuoteScraps(user.id, request.quoteIds)
        return ResponseEntity.noContent().build()
    }

    private companion object {
        const val UNAUTHORIZED_DESCRIPTION = "access token이 없거나, 만료되었거나, 유효하지 않습니다."
        const val QUOTE_NOT_FOUND_DESCRIPTION = "요청한 문장을 찾을 수 없습니다."
        const val SCRAPPED_QUOTES_DESCRIPTION =
            "로그인 사용자가 스크랩한 문장과 책 정보를 최신 스크랩 순으로 조회한다. " +
                "limit 기본값은 10, 최대값은 50이다."
        const val SCRAPPED_QUOTES_CURSOR_DESCRIPTION =
            "다음 페이지 조회 커서. 첫 페이지에서는 생략하고, " +
                "이후에는 직전 응답의 nextCursor를 그대로 전달합니다."
        const val BULK_DELETE_DESCRIPTION =
            "로그인 사용자의 스크랩 중 요청한 문장 ID 목록을 한 번에 취소한다. " +
                "한 번에 최대 50개까지 요청할 수 있다."
    }
}
