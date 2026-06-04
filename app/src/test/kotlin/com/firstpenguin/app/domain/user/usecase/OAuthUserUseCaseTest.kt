package com.firstpenguin.app.domain.user.usecase

import com.firstpenguin.app.domain.user.model.OAuthUserProfile
import com.firstpenguin.app.domain.user.model.Provider
import com.firstpenguin.app.domain.user.model.User
import com.firstpenguin.app.domain.user.model.UserStatus
import com.firstpenguin.app.domain.user.service.UserNicknameGenerator
import com.firstpenguin.app.domain.user.service.UserService
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OAuthUserUseCaseTest {
    private lateinit var userNicknameGenerator: UserNicknameGenerator
    private lateinit var userService: UserService
    private lateinit var oAuthUserUseCase: OAuthUserUseCase

    @BeforeEach
    fun setUp() {
        userNicknameGenerator = Mockito.mock(UserNicknameGenerator::class.java)
        userService = Mockito.mock(UserService::class.java)
        oAuthUserUseCase = OAuthUserUseCase(userNicknameGenerator, userService)
    }

    @Test
    fun `기존 OAuth 사용자는 닉네임 생성 없이 로그인 정보만 갱신한다`() {
        val user = user()
        Mockito.`when`(userService.findOAuthUser(OAUTH_USER_PROFILE)).thenReturn(user)
        Mockito.`when`(userService.updateOAuthLogin(user, OAUTH_USER_PROFILE)).thenReturn(user)

        val result = oAuthUserUseCase.loginOAuthUser(OAUTH_USER_PROFILE)

        assertEquals(user, result)
        Mockito.verify(userService).updateOAuthLogin(user, OAUTH_USER_PROFILE)
        Mockito.verifyNoInteractions(userNicknameGenerator)
    }

    @Test
    fun `신규 OAuth 사용자는 닉네임을 생성해 저장한다`() {
        val user = user(nickname = FIRST_NICKNAME)
        Mockito.`when`(userNicknameGenerator.generate()).thenReturn(FIRST_NICKNAME, SECOND_NICKNAME)
        Mockito.`when`(userService.findOAuthUser(OAUTH_USER_PROFILE)).thenReturn(null)
        Mockito.`when`(userService.createOAuthUser(OAUTH_USER_PROFILE, FIRST_NICKNAME)).thenReturn(user)

        val result = oAuthUserUseCase.loginOAuthUser(OAUTH_USER_PROFILE)

        assertEquals(user, result)
        Mockito.verify(userService).createOAuthUser(OAUTH_USER_PROFILE, FIRST_NICKNAME)
        Mockito.verify(userService, Mockito.never()).updateOAuthLogin(user, OAUTH_USER_PROFILE)
    }

    @Test
    fun `닉네임 중복이면 닉네임을 다시 생성해 OAuth 사용자를 생성한다`() {
        val user = user(nickname = SECOND_NICKNAME)
        Mockito.`when`(userNicknameGenerator.generate()).thenReturn(FIRST_NICKNAME, SECOND_NICKNAME)
        Mockito.`when`(userService.findOAuthUser(OAUTH_USER_PROFILE)).thenReturn(null)
        Mockito.`when`(userService.createOAuthUser(OAUTH_USER_PROFILE, FIRST_NICKNAME)).thenReturn(null)
        Mockito.`when`(userService.createOAuthUser(OAUTH_USER_PROFILE, SECOND_NICKNAME)).thenReturn(user)

        val result = oAuthUserUseCase.loginOAuthUser(OAUTH_USER_PROFILE)

        assertEquals(user, result)
        Mockito.verify(userService).createOAuthUser(OAUTH_USER_PROFILE, FIRST_NICKNAME)
        Mockito.verify(userService).createOAuthUser(OAUTH_USER_PROFILE, SECOND_NICKNAME)
    }

    @Test
    fun `동시 첫 로그인으로 provider unique 충돌이 발생하면 재조회 후 로그인 정보 갱신으로 수렴한다`() {
        val user = user()
        Mockito
            .`when`(userService.findOAuthUser(OAUTH_USER_PROFILE))
            .thenReturn(null, user)
        Mockito.`when`(userNicknameGenerator.generate()).thenReturn(FIRST_NICKNAME)
        Mockito.`when`(userService.createOAuthUser(OAUTH_USER_PROFILE, FIRST_NICKNAME)).thenReturn(null)
        Mockito.`when`(userService.updateOAuthLogin(user, OAUTH_USER_PROFILE)).thenReturn(user)

        val result = oAuthUserUseCase.loginOAuthUser(OAUTH_USER_PROFILE)

        assertEquals(user, result)
        Mockito.verify(userService).updateOAuthLogin(user, OAUTH_USER_PROFILE)
    }

    @Test
    fun `닉네임 생성 재시도 횟수를 초과하면 실패한다`() {
        Mockito.`when`(userNicknameGenerator.generate()).thenReturn(FIRST_NICKNAME)
        Mockito.`when`(userService.findOAuthUser(OAUTH_USER_PROFILE)).thenReturn(null)
        Mockito.`when`(userService.createOAuthUser(OAUTH_USER_PROFILE, FIRST_NICKNAME)).thenReturn(null)

        val exception =
            assertFailsWith<CustomException> {
                oAuthUserUseCase.loginOAuthUser(OAUTH_USER_PROFILE)
            }

        assertEquals(ErrorCode.NICKNAME_GENERATION_FAILED, exception.errorCode)
        Mockito.verify(userService, Mockito.times(10)).createOAuthUser(OAUTH_USER_PROFILE, FIRST_NICKNAME)
    }

    private fun user(nickname: String = FIRST_NICKNAME): User {
        val now = LocalDateTime.now()

        return User(
            id = USER_ID,
            provider = Provider.KAKAO,
            providerId = OAUTH_USER_PROFILE.providerId,
            email = OAUTH_USER_PROFILE.email,
            providerDisplayName = OAUTH_USER_PROFILE.providerDisplayName,
            nickname = nickname,
            profileImageId = null,
            status = UserStatus.ACTIVE,
            lastLoginAt = now,
            deletedAt = null,
            createdAt = now,
            updatedAt = now,
        )
    }

    private companion object {
        const val USER_ID = 1L
        const val FIRST_NICKNAME = "조용한토끼"
        const val SECOND_NICKNAME = "책읽는펭귄"
        val OAUTH_USER_PROFILE =
            OAuthUserProfile(
                provider = Provider.KAKAO,
                providerId = "oauth-user",
                email = "oauth@example.com",
                providerDisplayName = "OAuth User",
            )
    }
}
