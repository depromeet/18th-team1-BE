package com.firstpenguin.app.domain.user.controller

import com.firstpenguin.app.domain.auth.model.AuthenticatedUser
import com.firstpenguin.app.domain.user.dto.UpdateUserRequest
import com.firstpenguin.app.domain.user.dto.UserResponse
import com.firstpenguin.app.domain.user.usecase.UserUseCase
import com.firstpenguin.app.global.response.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
@Tag(name = "사용자", description = "로그인한 사용자 정보 API")
class UserController(
    private val userUseCase: UserUseCase,
) {
    @GetMapping("/me")
    @Operation(
        summary = "내 정보 조회",
        description = ME_DESCRIPTION,
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "내 정보 조회 성공",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = UserResponse::class),
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
    fun me(
        @Parameter(hidden = true)
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
    ): UserResponse = userUseCase.getMe(authenticatedUser.id)

    @PatchMapping("/me")
    @Operation(
        summary = "내 프로필 수정",
        description = UPDATE_ME_DESCRIPTION,
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "프로필 수정 성공",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = UserResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "nickname과 profileImageId가 모두 null이거나, nickname이 빈 문자열이거나, profileImageId가 존재하지 않습니다.",
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
    fun updateMe(
        @Parameter(hidden = true)
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @RequestBody request: UpdateUserRequest,
    ): UserResponse = userUseCase.updateMe(authenticatedUser.id, request)

    private companion object {
        const val ME_DESCRIPTION =
            "Authorization 헤더에 `Bearer {accessToken}` 형식으로 access token을 담아 호출합니다. " +
                "`profileImageUrl`은 `users.profile_image_id`로 연결된 이미지가 없으면 null입니다."

        const val UPDATE_ME_DESCRIPTION =
            "닉네임 또는 프로필 이미지를 수정합니다. " +
                "nickname, profileImageId 중 하나 이상은 반드시 포함해야 합니다. " +
                "null 필드는 변경하지 않습니다. 프로필 이미지 제거(null로 초기화)는 현재 미지원입니다. " +
                "profileImageId는 `POST /images/presigned-url`로 발급받은 imageId를 사용합니다."
    }
}
