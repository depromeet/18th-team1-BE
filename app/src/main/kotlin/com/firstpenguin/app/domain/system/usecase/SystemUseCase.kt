package com.firstpenguin.app.domain.system.usecase

import com.firstpenguin.app.domain.system.dto.EnvironmentResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

@Service
class SystemUseCase(
    private val environment: Environment,
    @Value("\${app.token.enabled:false}") private val devTokenEnabled: Boolean,
) {
    fun getEnvironment(): EnvironmentResponse {
        val activeProfiles = activeProfiles()
        return EnvironmentResponse(activeProfiles, activeProfiles.contains(PROD_PROFILE), devTokenEnabled)
    }

    private fun activeProfiles(): List<String> = environment.activeProfiles.toList().ifEmpty { listOf(DEFAULT_PROFILE) }

    private companion object {
        const val DEFAULT_PROFILE = "default"
        const val PROD_PROFILE = "prod"
    }
}
