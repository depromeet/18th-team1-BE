package com.firstpenguin.app.domain.auth.controller

import com.firstpenguin.app.domain.auth.dto.AccessTokenResponse
import com.firstpenguin.app.domain.auth.token.JwtTokenProvider
import com.firstpenguin.app.domain.user.model.OAuthUserProfile
import com.firstpenguin.app.domain.user.model.Provider
import com.firstpenguin.app.domain.user.repository.UserRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@ConditionalOnProperty("app.token.enabled", havingValue = "true")
@RequestMapping("/auth")
@Tag(name = "인증")
class DevAuthController(
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    @GetMapping("/dev-token")
    @Operation(summary = "[DEV] 개발용 토큰 발급", description = "dev 프로필에서만 활성화됩니다. 고정 더미 유저의 Access Token을 반환합니다.")
    fun devToken(): AccessTokenResponse {
        val devUser = userRepository.upsertOAuthUser(DEV_USER_PROFILE)
        return AccessTokenResponse(jwtTokenProvider.createAccessToken(devUser))
    }

    private companion object {
        val DEV_USER_PROFILE =
            OAuthUserProfile(
                provider = Provider.KAKAO,
                providerId = "dev-user",
                email = "dev@firstpenguin.com",
                nickname = "개발자",
            )
    }
}
