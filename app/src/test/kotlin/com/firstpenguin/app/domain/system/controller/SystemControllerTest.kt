package com.firstpenguin.app.domain.system.controller

import com.firstpenguin.app.domain.system.dto.EnvironmentResponse
import com.firstpenguin.app.domain.system.usecase.SystemUseCase
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import kotlin.test.assertSame

class SystemControllerTest {
    @Test
    fun `환경 확인은 usecase로 위임한다`() {
        val systemUseCase = Mockito.mock(SystemUseCase::class.java)
        val response = EnvironmentResponse(listOf("prod"), prod = true, devTokenEnabled = true)

        Mockito.`when`(systemUseCase.getEnvironment()).thenReturn(response)

        assertSame(response, SystemController(systemUseCase).getEnvironment())
        Mockito.verify(systemUseCase).getEnvironment()
    }
}
