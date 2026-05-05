package com.firstpenguin.app.domain.auth.token

import com.firstpenguin.app.domain.auth.model.AuthenticatedUser
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class JwtAuthenticationFilterTest {
    private lateinit var jwtAuthenticator: JwtAuthenticator
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.clearContext()
        jwtAuthenticator = Mockito.mock(JwtAuthenticator::class.java)
        jwtAuthenticationFilter = JwtAuthenticationFilter(jwtAuthenticator)
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `Bearer 토큰이 없으면 인증을 시도하지 않는다`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        var chainCalled = false

        jwtAuthenticationFilter.doFilter(request, response, FilterChain { _, _ -> chainCalled = true })

        assertTrue(chainCalled)
        assertNull(SecurityContextHolder.getContext().authentication)
        Mockito.verifyNoInteractions(jwtAuthenticator)
    }

    @Test
    fun `인증 성공 시 SecurityContext에 Authentication을 저장한다`() {
        val request = requestWithBearerToken()
        val response = MockHttpServletResponse()
        val authentication = authentication()
        Mockito.`when`(jwtAuthenticator.authenticate(TOKEN)).thenReturn(authentication)

        jwtAuthenticationFilter.doFilter(request, response, FilterChain { _, _ -> })

        assertSame(authentication, SecurityContextHolder.getContext().authentication)
        assertNull(request.getAttribute(JWT_AUTHENTICATION_ERROR_ATTRIBUTE))
    }

    @Test
    fun `인증 실패 시 인증 에러를 저장하고 SecurityContext를 비운다`() {
        val request = requestWithBearerToken()
        val response = MockHttpServletResponse()
        SecurityContextHolder.getContext().authentication = authentication()
        Mockito
            .`when`(jwtAuthenticator.authenticate(TOKEN))
            .thenThrow(CustomException(ErrorCode.AUTH_USER_DELETED))

        jwtAuthenticationFilter.doFilter(request, response, FilterChain { _, _ -> })

        assertNull(SecurityContextHolder.getContext().authentication)
        assertEquals(ErrorCode.AUTH_USER_DELETED, request.getAttribute(JWT_AUTHENTICATION_ERROR_ATTRIBUTE))
    }

    private fun requestWithBearerToken(): MockHttpServletRequest =
        MockHttpServletRequest().apply {
            addHeader("Authorization", "Bearer $TOKEN")
        }

    private fun authentication() = UsernamePasswordAuthenticationToken(AuthenticatedUser(USER_ID), null, emptyList())

    private companion object {
        const val TOKEN = "access-token"
        const val USER_ID = 1L
    }
}
