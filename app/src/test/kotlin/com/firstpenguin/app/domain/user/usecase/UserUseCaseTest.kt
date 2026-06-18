package com.firstpenguin.app.domain.user.usecase

import com.firstpenguin.app.domain.auth.service.RefreshTokenService
import com.firstpenguin.app.domain.image.service.ImageService
import com.firstpenguin.app.domain.user.dto.UpdateUserRequest
import com.firstpenguin.app.domain.user.model.OAuthAccount
import com.firstpenguin.app.domain.user.model.Provider
import com.firstpenguin.app.domain.user.model.User
import com.firstpenguin.app.domain.user.model.UserStatus
import com.firstpenguin.app.domain.user.service.OAuthUserService
import com.firstpenguin.app.domain.user.service.UserService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDateTime
import kotlin.test.assertEquals

class UserUseCaseTest {
    private lateinit var imageService: ImageService
    private lateinit var oAuthUserService: OAuthUserService
    private lateinit var refreshTokenService: RefreshTokenService
    private lateinit var userService: UserService
    private lateinit var userUseCase: UserUseCase

    @BeforeEach
    fun setUp() {
        imageService = Mockito.mock(ImageService::class.java)
        oAuthUserService = Mockito.mock(OAuthUserService::class.java)
        refreshTokenService = Mockito.mock(RefreshTokenService::class.java)
        userService = Mockito.mock(UserService::class.java)
        userUseCase = UserUseCase(imageService, oAuthUserService, refreshTokenService, userService)
    }

    @Test
    fun `내 정보 조회 응답에 OAuth provider와 가입한 날짜를 포함한다`() {
        val user = user()
        val oAuthAccount = oAuthAccount()
        Mockito.`when`(userService.getById(USER_ID)).thenReturn(user)
        Mockito.`when`(oAuthUserService.getActiveOAuthAccount(USER_ID)).thenReturn(oAuthAccount)

        val response = userUseCase.getMe(USER_ID)

        assertEquals(oAuthAccount.provider.name, response.provider)
        assertEquals(oAuthAccount.email, response.email)
        assertEquals(user.createdAt.toLocalDate(), response.createdAt)
        Mockito.verifyNoInteractions(imageService)
    }

    @Test
    fun `닉네임만 수정할 때 프로필 이미지 ID를 생략할 수 있다`() {
        val updatedUser = user().copy(nickname = UPDATED_NICKNAME)
        Mockito.`when`(userService.getById(USER_ID)).thenReturn(updatedUser)
        Mockito.`when`(oAuthUserService.getActiveOAuthAccount(USER_ID)).thenReturn(oAuthAccount())

        val response = userUseCase.updateMe(USER_ID, UpdateUserRequest(nickname = UPDATED_NICKNAME))

        assertEquals(UPDATED_NICKNAME, response.nickname)
        Mockito.verify(userService).updateProfile(USER_ID, UPDATED_NICKNAME, null)
        Mockito.verifyNoInteractions(imageService)
    }

    @Test
    fun `프로필 이미지만 수정할 때 닉네임을 생략할 수 있다`() {
        val updatedUser = user().copy(profileImageId = PROFILE_IMAGE_ID)
        Mockito.`when`(userService.getById(USER_ID)).thenReturn(updatedUser)
        Mockito.`when`(oAuthUserService.getActiveOAuthAccount(USER_ID)).thenReturn(oAuthAccount())
        Mockito.`when`(imageService.findUrlById(PROFILE_IMAGE_ID)).thenReturn(PROFILE_IMAGE_URL)

        val response = userUseCase.updateMe(USER_ID, UpdateUserRequest(profileImageId = PROFILE_IMAGE_ID))

        assertEquals(PROFILE_IMAGE_URL, response.profileImageUrl)
        Mockito.verify(imageService).validateExists(PROFILE_IMAGE_ID)
        Mockito.verify(userService).updateProfile(USER_ID, null, PROFILE_IMAGE_ID)
    }

    @Test
    fun `내 계정 탈퇴 요청 시 사용자 상태를 변경하고 모든 refresh token을 삭제한다`() {
        userUseCase.withdrawMe(USER_ID)

        Mockito.verify(userService).requestWithdrawal(USER_ID)
        Mockito.verify(refreshTokenService).logoutAll(USER_ID)
    }

    private fun user(): User =
        User(
            id = USER_ID,
            nickname = "penguin",
            profileImageId = null,
            status = UserStatus.ACTIVE,
            withdrawalRequestedAt = null,
            withdrawalDueAt = null,
            deletedAt = null,
            createdAt = CREATED_AT,
            updatedAt = CREATED_AT,
        )

    private fun oAuthAccount(): OAuthAccount =
        OAuthAccount(
            id = OAUTH_ACCOUNT_ID,
            userId = USER_ID,
            provider = Provider.KAKAO,
            providerId = "provider-id",
            email = "user@example.com",
            providerDisplayName = "provider user",
            lastLoginAt = CREATED_AT,
            disconnectedAt = null,
            createdAt = CREATED_AT,
            updatedAt = CREATED_AT,
        )

    private companion object {
        const val OAUTH_ACCOUNT_ID = 100L
        const val USER_ID = 1L
        const val PROFILE_IMAGE_ID = 10L
        const val PROFILE_IMAGE_URL = "https://cdn.example.com/profile.png"
        const val UPDATED_NICKNAME = "새닉네임"
        val CREATED_AT: LocalDateTime = LocalDateTime.of(2026, 6, 13, 14, 30)
    }
}
