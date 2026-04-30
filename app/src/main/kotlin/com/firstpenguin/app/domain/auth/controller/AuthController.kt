package com.firstpenguin.app.domain.auth.controller

import com.firstpenguin.app.domain.auth.config.AuthProperties
import com.firstpenguin.app.domain.auth.dto.AccessTokenResponse
import com.firstpenguin.app.domain.auth.token.RefreshTokenCookieManager
import com.firstpenguin.app.domain.auth.usecase.RefreshTokenUseCase
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import com.firstpenguin.app.global.response.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
@Tag(name = "인증", description = "OAuth 로그인 이후 토큰 재발급과 로그아웃 API")
class AuthController(
    private val refreshTokenCookieManager: RefreshTokenCookieManager,
    private val refreshTokenUseCase: RefreshTokenUseCase,
    private val authProperties: AuthProperties,
) {
    @PostMapping("/refresh")
    @Operation(
        summary = "Access Token 재발급",
        description = REFRESH_DESCRIPTION,
        parameters = [
            Parameter(
                name = "refresh_token",
                `in` = ParameterIn.COOKIE,
                required = true,
                description = "OAuth 로그인 성공 시 발급된 HttpOnly refresh token 쿠키",
            ),
        ],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Access Token 재발급 성공. refresh token 쿠키도 새 값으로 회전됩니다.",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = AccessTokenResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                description = "refresh token 쿠키가 없거나, 만료되었거나, 유효하지 않습니다.",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
    fun refresh(
        @Parameter(hidden = true)
        request: HttpServletRequest,
        @Parameter(hidden = true)
        response: HttpServletResponse,
    ): AccessTokenResponse {
        val refreshToken = request.refreshTokenCookieValue()

        if (refreshToken.isNullOrBlank()) {
            throw CustomException(ErrorCode.REFRESH_TOKEN_REQUIRED)
        }

        val tokenPair = refreshTokenUseCase.rotate(refreshToken)
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookieManager.create(tokenPair.refreshToken).toString())
        return AccessTokenResponse(tokenPair.accessToken)
    }

    @PostMapping("/logout")
    @Operation(
        summary = "로그아웃",
        description = LOGOUT_DESCRIPTION,
        parameters = [
            Parameter(
                name = "refresh_token",
                `in` = ParameterIn.COOKIE,
                required = false,
                description = "삭제할 refresh token 쿠키. 없더라도 쿠키 만료 응답은 내려갑니다.",
            ),
        ],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "로그아웃 성공. refresh token 쿠키가 만료됩니다."),
        ],
    )
    fun logout(
        @Parameter(hidden = true)
        request: HttpServletRequest,
    ): ResponseEntity<Unit> {
        val refreshToken = request.refreshTokenCookieValue()
        refreshToken?.takeIf { it.isNotBlank() }?.let(refreshTokenUseCase::logout)
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshTokenCookieManager.expire().toString()).build()
    }

    private fun HttpServletRequest.refreshTokenCookieValue(): String? =
        cookies
            ?.firstOrNull { it.name == authProperties.refreshToken.cookieName }
            ?.value

    private companion object {
        const val REFRESH_DESCRIPTION =
            "refresh token 쿠키로 새 access token을 발급합니다. " +
                "OAuth 로그인 직후 callback 화면뿐 아니라, access token 만료/앱 재진입 등 access token이 다시 필요할 때 호출합니다. " +
                "요청에는 `credentials: \"include\"`를 설정해 refresh token 쿠키를 포함해야 합니다. " +
                "응답 body의 `accessToken`은 이후 보호 API 요청의 Authorization Bearer 헤더에 사용합니다."
        const val LOGOUT_DESCRIPTION =
            "저장된 refresh token hash를 삭제하고 refresh token 쿠키를 만료시킵니다. " +
                "프론트는 `credentials: \"include\"`로 호출해야 쿠키가 함께 전송됩니다."
    }
}
