package com.firstpenguin.app.domain.auth.usecase

import com.firstpenguin.app.domain.auth.service.RefreshTokenService
import com.firstpenguin.app.domain.auth.token.JwtTokenProvider
import com.firstpenguin.app.domain.user.model.OAuthUserProfile
import com.firstpenguin.app.domain.user.model.Provider
import com.firstpenguin.app.domain.user.model.User
import com.firstpenguin.app.domain.user.model.UserStatus
import com.firstpenguin.app.domain.user.service.OAuthUserService
import com.firstpenguin.app.domain.user.service.UserService
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import com.firstpenguin.app.global.security.AdminBatchSecretValidator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DevAuthUseCaseTest {
    private lateinit var oAuthUserService: OAuthUserService
    private lateinit var adminBatchSecretValidator: AdminBatchSecretValidator
    private lateinit var userService: UserService
    private lateinit var refreshTokenService: RefreshTokenService
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var devAuthUseCase: DevAuthUseCase

    @BeforeEach
    fun setUp() {
        oAuthUserService = Mockito.mock(OAuthUserService::class.java)
        adminBatchSecretValidator = Mockito.mock(AdminBatchSecretValidator::class.java)
        userService = Mockito.mock(UserService::class.java)
        refreshTokenService = Mockito.mock(RefreshTokenService::class.java)
        jwtTokenProvider = Mockito.mock(JwtTokenProvider::class.java)
        devAuthUseCase =
            DevAuthUseCase(
                oAuthUserService,
                adminBatchSecretValidator,
                userService,
                refreshTokenService,
                jwtTokenProvider,
            )
    }

    @Test
    fun `개발용 토큰 발급은 기존 더미 사용자 access token만 반환한다`() {
        val user = user(DEV_USER_ID)
        Mockito.`when`(oAuthUserService.findOAuthUser(DEV_USER_PROFILE)).thenReturn(user)
        Mockito.`when`(oAuthUserService.updateOAuthLogin(user, DEV_USER_PROFILE)).thenReturn(user)
        Mockito.`when`(jwtTokenProvider.createAccessToken(user)).thenReturn(ACCESS_TOKEN)

        val response = devAuthUseCase.issueDevToken()

        assertEquals(ACCESS_TOKEN, response.accessToken)
        Mockito.verifyNoInteractions(adminBatchSecretValidator, userService, refreshTokenService)
    }

    @Test
    fun `임시 토큰 발급 시 관리자 secret 검증 후 9065 사용자 토큰을 발급한다`() {
        val user = user(TEMPORARY_LOGIN_USER_ID)
        Mockito.`when`(userService.getAuthenticatableById(USER_ID)).thenReturn(user)
        Mockito.`when`(refreshTokenService.issue(user)).thenReturn(REFRESH_TOKEN)
        Mockito.`when`(jwtTokenProvider.createAccessToken(user)).thenReturn(ACCESS_TOKEN)

        val tokenPair = devAuthUseCase.issueTemporaryLoginToken(ADMIN_SECRET)

        assertEquals(ACCESS_TOKEN, tokenPair.accessToken)
        assertEquals(REFRESH_TOKEN, tokenPair.refreshToken)
        Mockito.verify(adminBatchSecretValidator).validate(ADMIN_SECRET)
        Mockito.verify(userService).getAuthenticatableById(USER_ID)
    }

    @Test
    fun `관리자 secret 검증 실패 시 토큰을 발급하지 않는다`() {
        Mockito
            .doThrow(CustomException(ErrorCode.UNAUTHORIZED))
            .`when`(adminBatchSecretValidator)
            .validate(null)

        assertFailsWith<CustomException> { devAuthUseCase.issueTemporaryLoginToken(null) }

        Mockito.verifyNoInteractions(userService, refreshTokenService, jwtTokenProvider)
    }

    private fun user(id: Long): User {
        val now = LocalDateTime.now()

        return User(
            id = id,
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
        const val DEV_USER_ID = 1L
        const val TEMPORARY_LOGIN_USER_ID = 9065L
        const val USER_ID = TEMPORARY_LOGIN_USER_ID
        const val ADMIN_SECRET = "admin-secret"
        const val ACCESS_TOKEN = "access-token"
        const val REFRESH_TOKEN = "refresh-token"
        val DEV_USER_PROFILE =
            OAuthUserProfile(
                provider = Provider.KAKAO,
                providerId = "dev-user",
                email = "dev@firstpenguin.com",
                providerDisplayName = "개발자",
            )
    }
}
