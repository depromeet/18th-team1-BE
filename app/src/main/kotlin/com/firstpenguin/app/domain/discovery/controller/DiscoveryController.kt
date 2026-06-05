package com.firstpenguin.app.domain.discovery.controller

import com.firstpenguin.app.domain.auth.model.AuthenticatedUser
import com.firstpenguin.app.domain.discovery.dto.DiscoveryQuotesResponse
import com.firstpenguin.app.domain.discovery.usecase.DiscoveryUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/discovery")
@Tag(name = "발견탭", description = "추천 이력 기반 발견탭 API")
class DiscoveryController(
    private val discoveryUseCase: DiscoveryUseCase,
) {
    @Operation(
        summary = "발견탭 랜덤 문장 조회 API",
        description = "누군가에게 한 번이라도 추천된 문장을 랜덤으로 10개까지 조회한다.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @GetMapping("/quotes")
    fun getDiscoveryQuotes(
        @Parameter(hidden = true)
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
    ): DiscoveryQuotesResponse = discoveryUseCase.getDiscoveryQuotes(authenticatedUser.id)
}
