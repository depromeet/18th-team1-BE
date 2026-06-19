package com.firstpenguin.app.domain.system.controller

import com.firstpenguin.app.domain.system.dto.EnvironmentResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/system")
@Tag(name = "시스템", description = "시스템 상태 확인 API")
class SystemController(
    private val environment: Environment,
    @Value("\${app.token.enabled:false}") private val devTokenEnabled: Boolean,
) {
    @GetMapping("/environment")
    @Operation(summary = "실행 환경 확인 API", description = "민감정보 없이 active profile과 개발용 토큰 활성화 여부를 반환한다.")
    fun getEnvironment(): EnvironmentResponse {
        val activeProfiles = environment.activeProfiles.toList().ifEmpty { listOf(DEFAULT_PROFILE) }
        return EnvironmentResponse(activeProfiles, activeProfiles.contains(PROD_PROFILE), devTokenEnabled)
    }

    private companion object {
        const val DEFAULT_PROFILE = "default"
        const val PROD_PROFILE = "prod"
    }
}
