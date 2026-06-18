package com.firstpenguin.app.domain.auth.service

import com.firstpenguin.app.domain.auth.config.AuthProperties
import com.firstpenguin.app.domain.auth.model.RefreshToken
import com.firstpenguin.app.domain.auth.model.TokenClaims
import com.firstpenguin.app.domain.auth.repository.RefreshTokenRepository
import com.firstpenguin.app.domain.auth.token.JwtTokenProvider
import com.firstpenguin.app.domain.user.model.User
import com.firstpenguin.app.domain.user.model.UserStatus
import com.firstpenguin.app.domain.user.service.UserService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDateTime
import kotlin.test.assertEquals

class RefreshTokenServiceTest {
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var userService: UserService
    private lateinit var refreshTokenService: RefreshTokenService

    @BeforeEach
    fun setUp() {
        refreshTokenRepository = Mockito.mock(RefreshTokenRepository::class.java)
        jwtTokenProvider = Mockito.mock(JwtTokenProvider::class.java)
        userService = Mockito.mock(UserService::class.java)
        refreshTokenService =
            RefreshTokenService(
                refreshTokenRepository,
                jwtTokenProvider,
                userService,
                AuthProperties(),
            )
    }

    @Test
    fun `refresh token 재발급 시 인증 가능한 사용자를 조회한다`() {
        val user = user()
        Mockito.`when`(jwtTokenProvider.validateRefreshToken(REFRESH_TOKEN)).thenReturn(TokenClaims(USER_ID))
        Mockito.`when`(jwtTokenProvider.hash(REFRESH_TOKEN)).thenReturn(OLD_TOKEN_HASH)
        Mockito.`when`(refreshTokenRepository.findByTokenHash(OLD_TOKEN_HASH)).thenReturn(refreshToken())
        Mockito.`when`(userService.getAuthenticatableById(USER_ID)).thenReturn(user)
        Mockito.`when`(jwtTokenProvider.createRefreshToken(USER_ID)).thenReturn(NEW_REFRESH_TOKEN)
        Mockito.`when`(jwtTokenProvider.hash(NEW_REFRESH_TOKEN)).thenReturn(NEW_TOKEN_HASH)
        Mockito.`when`(jwtTokenProvider.createAccessToken(user)).thenReturn(ACCESS_TOKEN)

        val tokenPair = refreshTokenService.rotate(REFRESH_TOKEN)

        assertEquals(ACCESS_TOKEN, tokenPair.accessToken)
        assertEquals(NEW_REFRESH_TOKEN, tokenPair.refreshToken)
        Mockito.verify(userService).getAuthenticatableById(USER_ID)
        Mockito.verify(userService, Mockito.never()).getById(USER_ID)
    }

    private fun refreshToken(): RefreshToken {
        val now = LocalDateTime.now()

        return RefreshToken(
            id = REFRESH_TOKEN_ID,
            userId = USER_ID,
            deviceId = DEVICE_ID,
            tokenHash = OLD_TOKEN_HASH,
            expiresAt = now.plusDays(1),
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun user(): User {
        val now = LocalDateTime.now()

        return User(
            id = USER_ID,
            nickname = "penguin",
            profileImageId = null,
            status = UserStatus.ACTIVE,
            withdrawalRequestedAt = null,
            withdrawalDueAt = null,
            deletedAt = null,
            createdAt = now,
            updatedAt = now,
        )
    }

    private companion object {
        const val REFRESH_TOKEN_ID = 1L
        const val USER_ID = 10L
        const val DEVICE_ID = "device-id"
        const val REFRESH_TOKEN = "refresh-token"
        const val NEW_REFRESH_TOKEN = "new-refresh-token"
        const val OLD_TOKEN_HASH = "old-token-hash"
        const val NEW_TOKEN_HASH = "new-token-hash"
        const val ACCESS_TOKEN = "access-token"
    }
}
