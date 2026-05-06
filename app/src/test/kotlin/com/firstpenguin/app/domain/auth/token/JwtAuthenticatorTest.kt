package com.firstpenguin.app.domain.auth.token

import com.firstpenguin.app.domain.auth.model.AuthenticatedUser
import com.firstpenguin.app.domain.auth.model.TokenClaims
import com.firstpenguin.app.domain.user.service.UserService
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class JwtAuthenticatorTest {
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var userService: UserService
    private lateinit var jwtAuthenticator: JwtAuthenticator

    @BeforeEach
    fun setUp() {
        jwtTokenProvider = Mockito.mock(JwtTokenProvider::class.java)
        userService = Mockito.mock(UserService::class.java)
        jwtAuthenticator = JwtAuthenticator(jwtTokenProvider, userService)
    }

    @Test
    fun `유효한 토큰과 인증 가능한 사용자면 Authentication을 반환한다`() {
        Mockito.`when`(jwtTokenProvider.validateAccessToken(TOKEN)).thenReturn(TokenClaims(USER_ID))

        val authentication = jwtAuthenticator.authenticate(TOKEN)
        val principal = assertIs<AuthenticatedUser>(authentication.principal)

        assertEquals(USER_ID, principal.id)
        Mockito.verify(userService).validateAuthenticatableUser(USER_ID)
    }

    @Test
    fun `토큰 검증 실패는 그대로 전파한다`() {
        Mockito
            .`when`(jwtTokenProvider.validateAccessToken(TOKEN))
            .thenThrow(CustomException(ErrorCode.INVALID_ACCESS_TOKEN))

        val exception = assertFailsWith<CustomException> { jwtAuthenticator.authenticate(TOKEN) }

        assertEquals(ErrorCode.INVALID_ACCESS_TOKEN, exception.errorCode)
    }

    @Test
    fun `사용자 인증 가능성 검증 실패는 그대로 전파한다`() {
        Mockito.`when`(jwtTokenProvider.validateAccessToken(TOKEN)).thenReturn(TokenClaims(USER_ID))
        Mockito
            .doThrow(CustomException(ErrorCode.AUTH_USER_BLOCKED))
            .`when`(userService)
            .validateAuthenticatableUser(USER_ID)

        val exception = assertFailsWith<CustomException> { jwtAuthenticator.authenticate(TOKEN) }

        assertEquals(ErrorCode.AUTH_USER_BLOCKED, exception.errorCode)
    }

    private companion object {
        const val TOKEN = "access-token"
        const val USER_ID = 1L
    }
}
