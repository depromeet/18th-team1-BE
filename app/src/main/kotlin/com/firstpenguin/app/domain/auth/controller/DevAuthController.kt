package com.firstpenguin.app.domain.auth.controller

import com.firstpenguin.app.domain.auth.dto.AccessTokenResponse
import com.firstpenguin.app.domain.auth.usecase.DevAuthUseCase
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
    private val devAuthUseCase: DevAuthUseCase,
) {
    @GetMapping("/dev-token")
    @Operation(summary = "[DEV] 개발용 토큰 발급", description = "app.token.enabled=true일 때만 활성화. 고정 더미 유저의 Access Token 반환.")
    fun devToken(): AccessTokenResponse = devAuthUseCase.issueDevToken()
}
