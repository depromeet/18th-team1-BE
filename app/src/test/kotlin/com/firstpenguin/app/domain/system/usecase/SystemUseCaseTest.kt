package com.firstpenguin.app.domain.system.usecase

import org.junit.jupiter.api.Test
import org.springframework.mock.env.MockEnvironment
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SystemUseCaseTest {
    @Test
    fun `prod profile이면 prod true를 반환한다`() {
        val environment = MockEnvironment().apply { setActiveProfiles("prod") }
        val response = SystemUseCase(environment, true).getEnvironment()

        assertEquals(listOf("prod"), response.activeProfiles)
        assertTrue(response.prod)
        assertTrue(response.devTokenEnabled)
    }

    @Test
    fun `active profile이 없으면 default를 반환한다`() {
        val response = SystemUseCase(MockEnvironment(), false).getEnvironment()

        assertEquals(listOf("default"), response.activeProfiles)
        assertFalse(response.prod)
        assertFalse(response.devTokenEnabled)
    }
}
