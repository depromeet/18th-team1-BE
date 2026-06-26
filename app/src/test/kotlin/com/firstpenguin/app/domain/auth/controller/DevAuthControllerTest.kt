package com.firstpenguin.app.domain.auth.controller

import com.firstpenguin.app.domain.auth.dto.AccessTokenResponse
import com.firstpenguin.app.domain.auth.model.TokenPair
import com.firstpenguin.app.domain.auth.token.RefreshTokenCookieManager
import com.firstpenguin.app.domain.auth.usecase.DevAuthUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.assertEquals

class DevAuthControllerTest {
    private lateinit var devAuthUseCase: DevAuthUseCase
    private lateinit var refreshTokenCookieManager: RefreshTokenCookieManager
    private lateinit var devAuthController: DevAuthController

    @BeforeEach
    fun setUp() {
        devAuthUseCase = Mockito.mock(DevAuthUseCase::class.java)
        refreshTokenCookieManager = Mockito.mock(RefreshTokenCookieManager::class.java)
        devAuthController = DevAuthController(devAuthUseCase, refreshTokenCookieManager)
    }

    @Test
    fun `기존 개발용 토큰 API는 access token만 내려준다`() {
        Mockito.`when`(devAuthUseCase.issueDevToken()).thenReturn(AccessTokenResponse(ACCESS_TOKEN))

        val result = devAuthController.devToken()

        assertEquals(ACCESS_TOKEN, result.accessToken)
    }

    @Test
    fun `새 임시 로그인 토큰 API는 access token과 refresh token을 함께 내려준다`() {
        val response = MockHttpServletResponse()
        val tokenPair = TokenPair(ACCESS_TOKEN, REFRESH_TOKEN)
        val refreshTokenCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, REFRESH_TOKEN).build()
        Mockito.`when`(devAuthUseCase.issueTemporaryLoginToken(ADMIN_SECRET)).thenReturn(tokenPair)
        Mockito.`when`(refreshTokenCookieManager.create(REFRESH_TOKEN)).thenReturn(refreshTokenCookie)

        val result = devAuthController.temporaryLoginToken(ADMIN_SECRET, response)

        assertEquals(ACCESS_TOKEN, result.accessToken)
        assertEquals(REFRESH_TOKEN, result.refreshToken)
        assertEquals(refreshTokenCookie.toString(), response.getHeader(HttpHeaders.SET_COOKIE))
    }

    private companion object {
        const val ADMIN_SECRET = "admin-secret"
        const val ACCESS_TOKEN = "access-token"
        const val REFRESH_TOKEN = "refresh-token"
        const val REFRESH_TOKEN_COOKIE_NAME = "refresh_token"
    }
}
